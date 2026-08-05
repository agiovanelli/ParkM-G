package pmg.backend.parcheggio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.maps.MapsServiceImpl;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
import pmg.backend.posto.PostoService;
import pmg.backend.posto.StatoPosto;
import pmg.backend.posto.TipoPosto;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import pmg.backend.utente.UtenteService;

/**
 * Test unitari dell'implementazione del servizio parcheggi.
 *
 * I test verificano la nuova collaborazione con {@link PostoService}, che
 * gestisce i posti embedded nel documento {@link Parcheggio} senza utilizzare
 * un repository MongoDB dedicato ai posti.
 */
@ExtendWith(MockitoExtension.class)
class ParcheggioServiceImplTest {

    @Mock
    private ParcheggioRepository parcheggioRepository;

    @Mock
    private PrenotazioneRepository prenotazioneRepository;

    @Mock
    private LogService logService;

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private UtenteService utenteService;

    @Mock
    private AnaliticheRepository analiticheRepository;

    @Mock
    private PostoService postoService;

    @Mock
    private MapsServiceImpl mapsService;

    @InjectMocks
    private ParcheggioServiceImpl service;

    /** Verifica la ricerca dei parcheggi per area. */
    @Test
    void cercaPerAreaTest() {
        Parcheggio p1 = new Parcheggio(
                "A", "Centro", 100, 50, 45.5, 9.1);
        p1.setId("1");
        Parcheggio p2 = new Parcheggio(
                "B", "Centro", 80, 20, 45.6, 9.2);
        p2.setId("2");

        when(parcheggioRepository.findByAreaContainingIgnoreCase("Centro"))
                .thenReturn(List.of(p1, p2));

        List<ParcheggioResponse> result = service.cercaPerArea("Centro");

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals("A", result.get(0).nome());
        assertEquals("2", result.get(1).id());

        verify(parcheggioRepository)
                .findByAreaContainingIgnoreCase("Centro");
    }

    /** Verifica il flusso completo di creazione della prenotazione. */
    @Test
    void effettuaPrenotazioneTest() {
        Parcheggio parcheggio = new Parcheggio(
                "A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p1");

        Utente utente = org.mockito.Mockito.mock(Utente.class);
        when(utente.getId()).thenReturn("u1");

        Posto posto = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.PRENOTATO, false, 1);

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));
        when(utenteRepository.findById("u1"))
                .thenReturn(Optional.of(utente));
        when(utenteService.getPreferenze("u1"))
                .thenReturn(Map.of("distanza", "1"));
        when(postoService.prenotaPostoOttimale(
                "p1", Map.of("distanza", "1")))
                .thenReturn(posto);
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> {
                    Prenotazione prenotazione = invocation.getArgument(0);
                    prenotazione.setId("pr1");
                    return prenotazione;
                });

        PrenotazioneRequest request = new PrenotazioneRequest(
                "u1",
                "p1",
                LocalDateTime.of(2025, 1, 1, 10, 0),
                null,
                null);

        PrenotazioneResponse response = service.effettuaPrenotazione(request);

        assertNotNull(response);
        assertEquals("pr1", response.id());
        assertEquals("u1", response.utenteId());
        assertEquals("p1", response.parcheggioId());
        assertNotNull(response.codiceQr());
        assertNotNull(response.posto());
        assertEquals("1-01", response.posto().getSlotId());
        assertEquals(StatoPosto.PRENOTATO, response.posto().getStato());
        assertNotNull(response.scadenzaArrivo());

        verify(postoService).prenotaPostoOttimale(
                "p1", Map.of("distanza", "1"));
        verify(prenotazioneRepository).save(any(Prenotazione.class));
    }

    /** Verifica il blocco delle prenotazioni in stato di emergenza. */
    @Test
    void effettuaPrenotazioneParcheggioInEmergenzaTest() {
        Parcheggio parcheggio = new Parcheggio(
                "A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p1");
        parcheggio.setInEmergenza(true);

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        PrenotazioneRequest request = new PrenotazioneRequest(
                "u1", "p1", LocalDateTime.now(), null, null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.effettuaPrenotazione(request));

        assertEquals("Parcheggio in emergenza", exception.getMessage());
        verify(utenteRepository, never()).findById(any());
        verifyNoInteractions(postoService);
        verify(prenotazioneRepository, never()).save(any());
    }

    /** Verifica la gestione del caso in cui non esistano posti assegnabili. */
    @Test
    void effettuaPrenotazionePostiEsauritiTest() {
        Parcheggio parcheggio = new Parcheggio(
                "A", "Centro", 100, 0, 45.5, 9.1);
        parcheggio.setId("p2");

        Utente utente = org.mockito.Mockito.mock(Utente.class);
        when(utente.getId()).thenReturn("u1");

        Map<String, String> preferenze = Map.of("distanza", "1");

        when(parcheggioRepository.findById("p2"))
                .thenReturn(Optional.of(parcheggio));
        when(utenteRepository.findById("u1"))
                .thenReturn(Optional.of(utente));
        when(utenteService.getPreferenze("u1"))
                .thenReturn(preferenze);
        when(postoService.prenotaPostoOttimale("p2", preferenze))
                .thenReturn(null);

        PrenotazioneRequest request = new PrenotazioneRequest(
                "u1", "p2", LocalDateTime.now(), null, null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.effettuaPrenotazione(request));

        assertEquals("Posti esauriti", exception.getMessage());
        verify(prenotazioneRepository, never()).save(any());
    }

    /** Verifica la gestione di un parcheggio inesistente. */
    @Test
    void effettuaPrenotazioneParcheggioNonTrovatoTest() {
        when(parcheggioRepository.findById("missing"))
                .thenReturn(Optional.empty());

        PrenotazioneRequest request = new PrenotazioneRequest(
                "u1", "missing", LocalDateTime.now(), null, null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.effettuaPrenotazione(request));

        assertEquals("Parcheggio non trovato", exception.getMessage());
        verify(prenotazioneRepository, never()).save(any());
    }

    /** Verifica il filtro geografico applicato ai parcheggi vicini. */
    @Test
    void cercaViciniTest() {
        Parcheggio vicino = new Parcheggio(
                "Vicino", "Centro", 50, 10, 45.50, 9.20);
        vicino.setId("v1");

        Parcheggio lontano = new Parcheggio(
                "Lontano", "Centro", 50, 10, 46.00, 10.00);
        lontano.setId("l1");

        Parcheggio coordinateNonValide = new Parcheggio(
                "Senza coordinate", "Centro", 10, 10, 0, 0);
        coordinateNonValide.setId("x1");

        when(parcheggioRepository.findAll())
                .thenReturn(List.of(vicino, lontano, coordinateNonValide));

        List<ParcheggioResponse> result = service.cercaVicini(
                45.50, 9.20, 500.0);

        assertEquals(1, result.size());
        assertEquals("v1", result.get(0).id());
        assertEquals("Vicino", result.get(0).nome());
    }

    /** Verifica l'attivazione dell'emergenza e la creazione del log. */
    @Test
    void impostaStatoEmergenzaAttivaTest() {
        Parcheggio parcheggio = new Parcheggio(
                "Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        parcheggio.setId("p1");

        Analitiche analitica = org.mockito.Mockito.mock(Analitiche.class);
        when(analitica.getId()).thenReturn("a1");

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));

        service.impostaStatoEmergenza("p1", true, "Incendio");

        assertTrue(parcheggio.isInEmergenza());
        verify(parcheggioRepository).save(parcheggio);
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica la disattivazione dell'emergenza senza generare log. */
    @Test
    void impostaStatoEmergenzaDisattivaTest() {
        Parcheggio parcheggio = new Parcheggio(
                "Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        parcheggio.setId("p1");
        parcheggio.setInEmergenza(true);

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.impostaStatoEmergenza("p1", false, null);

        assertFalse(parcheggio.isInEmergenza());
        verify(logService, never()).salvaLog(any(LogRequest.class));
    }

    /** Verifica la delega dell'assegnazione ottimale al servizio posti. */
    @Test
    void assegnaPostoOttimaleNessunPostoTest() {
        Map<String, String> preferenze = Map.of("distanza", "1");
        when(postoService.prenotaPostoOttimale("p1", preferenze))
                .thenReturn(null);

        Posto result = service.assegnaPostoOttimale("p1", preferenze);

        assertNull(result);
        verify(postoService).prenotaPostoOttimale("p1", preferenze);
    }

    /** Verifica il recupero sintetico del parcheggio. */
    @Test
    void getByIdTest() {
        Parcheggio parcheggio = new Parcheggio(
                "Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        parcheggio.setId("p1");

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        ParcheggioResponse response = service.getById("p1");

        assertEquals("p1", response.id());
        assertEquals("Parcheggio A", response.nome());
        assertEquals("Centro", response.area());
        assertEquals(100, response.postiTotali());
    }

    /** Verifica l'errore restituito per un parcheggio inesistente. */
    @Test
    void getByIdNonTrovatoTest() {
        when(parcheggioRepository.findById("missing"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getById("missing"));

        assertEquals("Parcheggio non trovato", exception.getMessage());
    }

    /** Verifica la costruzione della mappa logica completa. */
    @Test
    void getMappaTest() {
        Parcheggio parcheggio = creaParcheggioConfigurato();

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        MappaParcheggioResponse response = service.getMappa("p1");

        assertEquals("p1", response.id());
        assertEquals(1, response.numPiani());
        assertEquals(2, response.postiTotali());
        assertEquals(1, response.configurazionePiani().size());
        assertEquals("1-01",
                response.configurazionePiani().get(0).posti().get(0)
                        .getSlotId());

        verify(postoService).generaPosti("p1");
    }

    /** Verifica la delega del recupero dei posti al servizio dedicato. */
    @Test
    void getPostiConPianoTest() {
        Posto posto = creaPosto(
                "2-05", 5, 2, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 1);
        PostoResponse dto = new PostoResponse(posto, "p1");

        when(postoService.getPostiByParcheggio("p1", 2))
                .thenReturn(List.of(dto));

        List<PostoResponse> result = service.getPosti("p1", 2);

        assertEquals(1, result.size());
        assertEquals("2-05", result.get(0).getSlotId());
        verify(postoService).getPostiByParcheggio("p1", 2);
    }

    /** Verifica il ricalcolo delle statistiche dai posti embedded. */
    @Test
    void syncPostiStatsTest() {
        Posto libero = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 1);
        Posto prenotato = creaPosto(
                "1-02", 2, 1, TipoPosto.NORMALE,
                StatoPosto.PRENOTATO, false, 2);
        Posto fuoriServizio = creaPosto(
                "1-03", 3, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, true, 3);

        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setId("p1");
        parcheggio.setConfigurazionePiani(List.of(
                new ConfigurazionePiano(
                        1,
                        3,
                        List.of(libero, prenotato, fuoriServizio))));

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.syncPostiStats("p1");

        assertEquals(3, parcheggio.getPostiTotali());
        assertEquals(1, parcheggio.getPostiDisponibili());
        assertEquals(1, parcheggio.getNumPiani());
        verify(postoService).generaPosti("p1");
        verify(parcheggioRepository).save(parcheggio);
    }

    /** Verifica l'errore di sincronizzazione per un parcheggio inesistente. */
    @Test
    void syncPostiStatsParcheggioNonTrovatoTest() {
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.syncPostiStats("p1"));

        assertEquals("Parcheggio non trovato", exception.getMessage());
        verify(postoService).generaPosti("p1");
        verify(parcheggioRepository, never()).save(any());
    }

    /** Crea un parcheggio con un piano e due posti già generati. */
    private Parcheggio creaParcheggioConfigurato() {
        Posto primo = creaPosto(
                "1-01", 1, 1, TipoPosto.NORMALE,
                StatoPosto.LIBERO, false, 1);
        Posto secondo = creaPosto(
                "1-02", 2, 1, TipoPosto.DISABILI,
                StatoPosto.LIBERO, false, 2);

        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setId("p1");
        parcheggio.setNome("Parcheggio Test");
        parcheggio.setConfigurazionePiani(List.of(
                new ConfigurazionePiano(1, 2, List.of(primo, secondo))));
        parcheggio.ricalcolaStatistiche();
        return parcheggio;
    }

    /** Crea un posto embedded utilizzato nei test. */
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
