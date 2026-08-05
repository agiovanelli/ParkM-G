package pmg.backend.posto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.parcheggio.ConfigurazionePiano;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

/**
 * Test unitari del servizio che gestisce i posti embedded nei parcheggi.
 */
@ExtendWith(MockitoExtension.class)
class PostoServiceImplTest {

    /** Repository dei parcheggi simulato. */
    @Mock
    private ParcheggioRepository parcheggioRepository;

    /** Servizio sottoposto a test. */
    @InjectMocks
    private PostoServiceImpl postoService;

    /** Verifica il recupero di tutti i posti ordinati per piano e numero. */
    @Test
    void getPostiByParcheggio_senzaPiano() {
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(
                        new ConfigurazionePiano(1, 2, List.of()),
                        new ConfigurazionePiano(2, 1, List.of())));
        configuraRepository(parcheggio);

        List<PostoResponse> result = postoService.getPostiByParcheggio("p1");

        assertEquals(3, result.size());
        assertEquals("1-01", result.get(0).getSlotId());
        assertEquals("1-02", result.get(1).getSlotId());
        assertEquals("2-01", result.get(2).getSlotId());
    }

    /** Verifica il recupero dei posti filtrati per piano. */
    @Test
    void getPostiByParcheggio_conPiano() {
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(
                        new ConfigurazionePiano(1, 2, List.of()),
                        new ConfigurazionePiano(2, 2, List.of())));
        configuraRepository(parcheggio);

        List<PostoResponse> result = postoService
                .getPostiByParcheggio("p1", 2);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(posto -> posto.getPiano() == 2));
    }

    /** Verifica la generazione di cento posti distribuiti su quattro piani. */
    @Test
    void generaPosti_ok() {
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(
                        new ConfigurazionePiano(1, 30, List.of()),
                        new ConfigurazionePiano(2, 25, List.of()),
                        new ConfigurazionePiano(3, 25, List.of()),
                        new ConfigurazionePiano(4, 20, List.of())));
        configuraRepository(parcheggio);

        postoService.generaPosti("p1");

        assertEquals(100, parcheggio.getTuttiPosti().size());
        assertEquals(100, parcheggio.getPostiTotali());
        assertEquals(100, parcheggio.getPostiDisponibili());
        assertEquals(4, parcheggio.getNumPiani());
        assertEquals("1-01", parcheggio.getTuttiPosti().get(0).getSlotId());
        assertEquals("4-20",
                parcheggio.getConfigurazionePiani().get(3)
                        .getPosti().get(19).getSlotId());
        assertEquals(
                TipoPosto.DISABILI,
                parcheggio.trovaPosto("1-30").orElseThrow().getTipo());
        assertEquals(
                TipoPosto.INCINTA,
                parcheggio.trovaPosto("1-28").orElseThrow().getTipo());

        verify(parcheggioRepository).save(parcheggio);
    }

    /** Verifica che la rigenerazione conservi stato e caratteristiche esistenti. */
    @Test
    void generaPosti_idempotenteConservaPostiEsistenti() {
        Posto esistente = creaPosto(
                "1-01",
                1,
                1,
                TipoPosto.DISABILI,
                StatoPosto.OCCUPATO,
                false,
                4);
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(
                        1, 3, List.of(esistente))));
        configuraRepository(parcheggio);

        postoService.generaPosti("p1");

        Posto mantenuto = parcheggio.trovaPosto("1-01").orElseThrow();
        assertEquals(TipoPosto.DISABILI, mantenuto.getTipo());
        assertEquals(StatoPosto.OCCUPATO, mantenuto.getStato());
        assertEquals(3, parcheggio.getTuttiPosti().size());
    }

    /** Verifica il rifiuto della generazione senza configurazione dei piani. */
    @Test
    void generaPosti_configurazioneAssente() {
        Parcheggio parcheggio = creaParcheggio("p1", List.of());
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> postoService.generaPosti("p1"));

        assertTrue(exception.getMessage().contains("Configurazione piani assente"));
        verify(parcheggioRepository, never()).save(any());
    }

    /** Verifica il rifiuto di un numero di piano non valido. */
    @Test
    void generaPosti_pianoNonValido() {
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(0, 1, List.of())));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        assertThrows(
                IllegalStateException.class,
                () -> postoService.generaPosti("p1"));
    }

    /** Verifica l'aggiornamento legacy della disponibilità. */
    @Test
    void aggiornaDisponibilita_cambiaValore() {
        Parcheggio parcheggio = creaParcheggioConPostoLibero();
        configuraRepository(parcheggio);

        PostoResponse response = postoService.aggiornaDisponibilita(
                "p1", 1, 1, false);

        assertFalse(response.isDisponibile());
        assertEquals(StatoPosto.PRENOTATO, response.getStato());
        assertEquals(0, parcheggio.getPostiDisponibili());
        verify(parcheggioRepository, atLeastOnce()).save(parcheggio);
    }

    /** Verifica l'errore durante l'aggiornamento di un posto inesistente. */
    @Test
    void aggiornaDisponibilita_postoNonTrovato() {
        Parcheggio parcheggio = creaParcheggioConPostoLibero();
        configuraRepository(parcheggio);

        assertThrows(
                IllegalArgumentException.class,
                () -> postoService.aggiornaDisponibilita(
                        "p1", 1, 2, true));
    }

    /** Verifica la messa fuori servizio di un posto libero. */
    @Test
    void aggiornaDisabilitato_ok() {
        Parcheggio parcheggio = creaParcheggioConPostoLibero();
        configuraRepository(parcheggio);

        PostoResponse response = postoService.aggiornaDisabilitato(
                "p1", 1, 1, true);

        assertTrue(response.isDisabilitato());
        assertTrue(response.isFuoriServizio());
        assertFalse(response.isDisponibile());
        assertEquals(0, parcheggio.getPostiDisponibili());
    }

    /** Verifica che un posto prenotato non possa essere messo fuori servizio. */
    @Test
    void aggiornaDisabilitato_postoPrenotato() {
        Posto prenotato = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.PRENOTATO, false, 1);
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(
                        1, 1, List.of(prenotato))));
        configuraRepository(parcheggio);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> postoService.aggiornaDisabilitato(
                        "p1", 1, 1, true));

        assertTrue(exception.getMessage().contains("solo un posto libero"));
    }

    /** Verifica l'aggiornamento dello stato tramite identificativo logico. */
    @Test
    void aggiornaStato_ok() {
        Parcheggio parcheggio = creaParcheggioConPostoLibero();
        configuraRepository(parcheggio);

        PostoResponse response = postoService.aggiornaStato(
                "p1", "1-01", StatoPosto.OCCUPATO);

        assertEquals(StatoPosto.OCCUPATO, response.getStato());
        assertFalse(response.isDisponibile());
        assertEquals(0, parcheggio.getPostiDisponibili());
    }

    /** Verifica il rifiuto di uno stato nullo. */
    @Test
    void aggiornaStato_statoNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> postoService.aggiornaStato("p1", "1-01", null));

        verify(parcheggioRepository, never()).findById(any());
    }

    /** Verifica che un posto fuori servizio non possa diventare occupato. */
    @Test
    void aggiornaStato_postoFuoriServizio() {
        Posto posto = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, true, 1);
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(1, 1, List.of(posto))));
        configuraRepository(parcheggio);

        assertThrows(
                IllegalStateException.class,
                () -> postoService.aggiornaStato(
                        "p1", "1-01", StatoPosto.OCCUPATO));
    }

    /** Verifica la prenotazione del posto normale con distanza migliore. */
    @Test
    void prenotaPostoOttimale_ok() {
        Posto primo = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 1);
        Posto secondo = creaPosto(
                "1-02", 2, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 4);
        Parcheggio parcheggio = creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(
                        1, 2, List.of(primo, secondo))));
        configuraRepository(parcheggio);

        Posto risultato = postoService.prenotaPostoOttimale(
                "p1", Map.of("distanza", "2"));

        assertNotNull(risultato);
        assertEquals("1-01", risultato.getSlotId());
        assertEquals(StatoPosto.PRENOTATO, risultato.getStato());
        assertEquals(1, parcheggio.getPostiDisponibili());
    }

    /** Verifica la ricerca diretta di uno slot embedded. */
    @Test
    void trovaPosto_ok() {
        Parcheggio parcheggio = creaParcheggioConPostoLibero();
        configuraRepository(parcheggio);

        Posto risultato = postoService.trovaPosto("p1", "1-01");

        assertEquals("1-01", risultato.getSlotId());
    }

    /** Configura il repository affinché restituisca sempre lo stesso documento. */
    private void configuraRepository(Parcheggio parcheggio) {
        when(parcheggioRepository.findById(parcheggio.getId()))
                .thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Crea un parcheggio con un solo posto libero. */
    private Parcheggio creaParcheggioConPostoLibero() {
        Posto posto = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 1);
        return creaParcheggio(
                "p1",
                List.of(new ConfigurazionePiano(
                        1, 1, List.of(posto))));
    }

    /** Crea un documento parcheggio con la configurazione specificata. */
    private Parcheggio creaParcheggio(
            String id,
            List<ConfigurazionePiano> configurazionePiani) {
        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setId(id);
        parcheggio.setNome("Parcheggio Test");
        parcheggio.setConfigurazionePiani(configurazionePiani);
        parcheggio.ricalcolaStatistiche();
        return parcheggio;
    }

    /** Crea un posto embedded. */
    private Posto creaPosto(
            String slotId,
            int numero,
            int piano,
            TipoPosto tipo,
            StatoPosto stato,
            boolean fuoriServizio,
            int distanzaUscita) {
        return new Posto(
                slotId,
                numero,
                "P" + piano + "-" + String.format("%02d", numero),
                piano,
                tipo,
                stato,
                fuoriServizio,
                distanzaUscita);
    }
}
