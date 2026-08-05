package pmg.backend.parcheggio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoServiceImpl;
import pmg.backend.posto.StatoPosto;
import pmg.backend.posto.TipoPosto;

/**
 * Verifica l'algoritmo di assegnazione del posto ottimale senza collegarsi a MongoDB.
 *
 * Il repository viene sostituito da un mock Mockito. Di conseguenza i test non
 * cancellano, inseriscono o modificano documenti presenti nel database reale.
 * Il nome della classe è mantenuto per consentire la sostituzione diretta del
 * precedente file di integrazione.
 */
@ExtendWith(MockitoExtension.class)
class PostoOttimaleIntegrationTest {

    /** Repository simulato: non esegue operazioni reali su MongoDB. */
    @Mock
    private ParcheggioRepository parcheggioRepository;

    /** Servizio sottoposto a test. */
    private PostoServiceImpl postoService;

    /** Inizializza il servizio utilizzando esclusivamente il repository simulato. */
    @BeforeEach
    void setup() {
        postoService = new PostoServiceImpl(parcheggioRepository);
        when(parcheggioRepository.save(any(Parcheggio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Verifica che un utente disabile riceva un posto riservato. */
    @Test
    void assegnaPostoOttimale_disabile_scegliePostoRiservato() {
        Parcheggio parcheggio = creaParcheggio(
                "P1",
                List.of(
                        creaPosto("1-01", 1, TipoPosto.NORMALE, 1),
                        creaPosto("1-02", 2, TipoPosto.DISABILI, 4)));
        preparaRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale(
                "P1",
                Map.of("disabile", "Si", "distanza", "2"));

        assertNotNull(risultato);
        assertTrue(risultato.isRiservatoDisabili());
        assertEquals("1-02", risultato.getSlotId());
        assertEquals(StatoPosto.PRENOTATO, risultato.getStato());
        assertEquals(
                StatoPosto.PRENOTATO,
                parcheggio.trovaPosto("1-02").orElseThrow().getStato());
        verify(parcheggioRepository, atLeastOnce()).save(parcheggio);
    }

    /** Verifica che un utente non disabile non riceva un posto riservato. */
    @Test
    void assegnaPostoOttimale_nonDisabile_evitaPostoDisabili() {
        Parcheggio parcheggio = creaParcheggio(
                "P2",
                List.of(
                        creaPosto("1-01", 1, TipoPosto.DISABILI, 1),
                        creaPosto("1-02", 2, TipoPosto.NORMALE, 2)));
        preparaRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale(
                "P2",
                Map.of("disabile", "No"));

        assertNotNull(risultato);
        assertFalse(risultato.isRiservatoDisabili());
        assertEquals("1-02", risultato.getSlotId());
    }

    /** Verifica che una donna incinta riceva un posto dedicato. */
    @Test
    void assegnaPostoOttimale_incinta_scegliePostoRiservato() {
        Parcheggio parcheggio = creaParcheggio(
                "P3",
                List.of(
                        creaPosto("1-01", 1, TipoPosto.NORMALE, 1),
                        creaPosto("1-02", 2, TipoPosto.INCINTA, 3)));
        preparaRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale(
                "P3",
                Map.of("donnaIncinta", "Si"));

        assertNotNull(risultato);
        assertTrue(risultato.isRiservatoIncinta());
        assertEquals("1-02", risultato.getSlotId());
    }

    /** Verifica la selezione del posto con distanza più vicina alla preferenza. */
    @Test
    void assegnaPostoOttimale_sceglieDistanzaMigliore() {
        Parcheggio parcheggio = creaParcheggio(
                "P4",
                List.of(
                        creaPosto("1-01", 1, TipoPosto.NORMALE, 1),
                        creaPosto("1-02", 2, TipoPosto.NORMALE, 5)));
        preparaRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale(
                "P4",
                Map.of("distanza", "2"));

        assertNotNull(risultato);
        assertEquals(1, risultato.getDistanzaUscita());
        assertEquals("1-01", risultato.getSlotId());
    }

    /** Verifica il risultato nullo quando il parcheggio non ha posti. */
    @Test
    void assegnaPostoOttimale_nessunPosto_returnNull() {
        Parcheggio parcheggio = creaParcheggio("P5", List.of());
        preparaRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale("P5", Map.of());

        assertNull(risultato);
    }

    /** Configura il mock affinché restituisca il parcheggio indicato. */
    private void preparaRepository(Parcheggio parcheggio) {
        when(parcheggioRepository.findById(parcheggio.getId()))
                .thenReturn(Optional.of(parcheggio));
    }

    /** Crea un parcheggio con un singolo piano e i posti specificati. */
    private Parcheggio creaParcheggio(String id, List<Posto> posti) {
        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setId(id);
        parcheggio.setNome("Parcheggio Test");
        parcheggio.setArea("Milano");
        parcheggio.setConfigurazionePiani(List.of(
                new ConfigurazionePiano(1, posti.size(), posti)));
        parcheggio.ricalcolaStatistiche();
        return parcheggio;
    }

    /** Crea un posto embedded libero. */
    private Posto creaPosto(
            String slotId,
            int numero,
            TipoPosto tipo,
            int distanzaUscita) {
        return new Posto(
                slotId,
                numero,
                "P1-" + String.format("%02d", numero),
                1,
                tipo,
                StatoPosto.LIBERO,
                false,
                distanzaUscita);
    }
}
