package pmg.backend.prenotazione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.exception.ConflictException;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
import pmg.backend.posto.PostoService;
import pmg.backend.posto.StatoPosto;
import pmg.backend.posto.TipoPosto;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;

/**
 * Test unitari del servizio che gestisce il ciclo di vita delle prenotazioni.
 *
 * I test verificano che annullamento, scadenza, conferma e uscita aggiornino
 * lo stato del posto embedded tramite {@link PostoService}.
 */
@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceImplTest {

    @Mock
    private PrenotazioneRepository prenotazioneRepository;

    @Mock
    private ParcheggioRepository parcheggioRepository;

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private LogService logService;

    @Mock
    private AnaliticheRepository analiticheRepository;

    @Mock
    private PostoService postoService;

    @InjectMocks
    private PrenotazioneServiceImpl service;

    private Clock fixedClock;
    private LocalDateTime now;
    private Prenotazione prenotazione;

    /** Configura una prenotazione base e un clock deterministico. */
    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(
                LocalDateTime.of(2025, 1, 15, 10, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault());
        service.setClock(fixedClock);
        now = LocalDateTime.now(fixedClock);

        prenotazione = new Prenotazione();
        prenotazione.setId("1");
        prenotazione.setUtenteId("u1");
        prenotazione.setParcheggioId("p1");
        prenotazione.setCodiceQr("QR1");
        prenotazione.setDataCreazione(now.minusMinutes(5));
        prenotazione.setScadenzaArrivo(now.plusMinutes(10));
        prenotazione.setStato(StatoPrenotazione.attiva);
    }

    /** Verifica il mapping dello storico utente. */
    @Test
    void getStoricoUtente_ok() {
        when(prenotazioneRepository.findByUtenteId("u1"))
                .thenReturn(List.of(prenotazione));

        List<PrenotazioneResponse> result =
                service.getStoricoUtente("u1");

        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).utenteId());
    }

    /** Verifica il recupero delle prenotazioni associate a un parcheggio. */
    @Test
    void getByParcheggio_ok() {
        when(prenotazioneRepository.findByParcheggioId("p1"))
                .thenReturn(List.of(prenotazione));

        List<PrenotazioneResponse> result =
                service.getByParcheggio("p1");

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).parcheggioId());
    }

    /** Verifica la validazione corretta dell'ingresso. */
    @Test
    void validaIngresso_ok() {
        configuraAnalitica("p1");
        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response = service.validaIngresso("QR1");

        assertEquals(StatoPrenotazione.inCorso, response.stato());
        assertEquals(now, response.dataIngresso());
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica l'errore per un codice QR inesistente. */
    @Test
    void validaIngresso_qrNonValido() {
        when(prenotazioneRepository.findByCodiceQr("QR404"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> service.validaIngresso("QR404"));
    }

    /** Verifica il rifiuto di una prenotazione in stato non valido. */
    @Test
    void validaIngresso_statoNonValido() {
        prenotazione.setStato(StatoPrenotazione.scaduta);
        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                ConflictException.class,
                () -> service.validaIngresso("QR1"));
    }

    /** Verifica che una prenotazione oltre la scadenza liberi il posto. */
    @Test
    void validaIngresso_scadenzaSuperata() {
        prenotazione.setScadenzaArrivo(now.minusSeconds(1));
        prenotazione.setPosto(creaSnapshot("1-01", StatoPosto.PRENOTATO));

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
                ConflictException.class,
                () -> service.validaIngresso("QR1"));

        assertEquals(StatoPrenotazione.scaduta, prenotazione.getStato());
        assertEquals(StatoPosto.LIBERO,
                prenotazione.getPosto().getStato());
        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.LIBERO);
    }

    /** Verifica l'annullamento e la liberazione dello slot embedded. */
    @Test
    void annullaPrenotazione_ok() {
        prenotazione.setPosto(creaSnapshot(
                "1-01", StatoPosto.PRENOTATO));
        configuraAnalitica("p1");

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response =
                service.annullaPrenotazione("1", "u1");

        assertEquals(StatoPrenotazione.annullata, response.stato());
        assertEquals(StatoPosto.LIBERO, response.posto().getStato());
        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.LIBERO);
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica l'annullamento di una prenotazione senza posto associato. */
    @Test
    void annullaPrenotazione_senzaPosto() {
        configuraAnalitica("p1");
        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response =
                service.annullaPrenotazione("1", "u1");

        assertEquals(StatoPrenotazione.annullata, response.stato());
        verify(postoService, never()).aggiornaStato(any(), any(), any());
    }

    /** Verifica l'errore quando la prenotazione da annullare non esiste. */
    @Test
    void annullaPrenotazione_notFound() {
        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }

    /** Verifica il rifiuto dell'annullamento dopo il pagamento. */
    @Test
    void annullaPrenotazione_statoNonValido() {
        prenotazione.setStato(StatoPrenotazione.pagato);
        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                IllegalStateException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }

    /** Verifica lo scheduler delle prenotazioni scadute. */
    @Test
    void controllaPrenotazioniScadute_postoLiberato() {
        prenotazione.setPosto(creaSnapshot(
                "1-01", StatoPosto.PRENOTATO));

        when(prenotazioneRepository.findByStatoAndScadenzaArrivoBefore(
                StatoPrenotazione.attiva, now))
                .thenReturn(List.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.controllaPrenotazioniScadute();

        assertEquals(StatoPrenotazione.scaduta, prenotazione.getStato());
        assertEquals(StatoPosto.LIBERO,
                prenotazione.getPosto().getStato());
        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.LIBERO);
        verify(prenotazioneRepository).save(prenotazione);
    }

    /** Verifica lo scheduler quando la prenotazione non ha un posto associato. */
    @Test
    void controllaPrenotazioniScadute_senzaPosto() {
        when(prenotazioneRepository.findByStatoAndScadenzaArrivoBefore(
                StatoPrenotazione.attiva, now))
                .thenReturn(List.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.controllaPrenotazioniScadute();

        assertEquals(StatoPrenotazione.scaduta, prenotazione.getStato());
        verify(postoService, never()).aggiornaStato(any(), any(), any());
    }

    /** Verifica la tariffa base con clock deterministico. */
    @Test
    void calcolaImporto_base() {
        prenotazione.setDataIngresso(now.minusHours(2));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(utenteRepository.findById("u1"))
                .thenReturn(Optional.empty());
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.empty());

        double result = service.calcolaImporto("1");

        assertEquals(6.0, result, 0.01);
    }

    /** Verifica il risultato nullo prima dell'ingresso. */
    @Test
    void calcolaImporto_noIngresso() {
        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertEquals(0.0, service.calcolaImporto("1"), 0.01);
    }

    /** Verifica lo sconto under 30. */
    @Test
    void calcolaImporto_conSconto() {
        prenotazione.setDataIngresso(now.minusHours(2));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));

        Utente utente = org.mockito.Mockito.mock(Utente.class);
        when(utente.getPreferenze())
                .thenReturn(Map.of("eta", "under30"));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(utenteRepository.findById("u1"))
                .thenReturn(Optional.of(utente));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.empty());

        double result = service.calcolaImporto("1");

        assertEquals(5.4, result, 0.01);
    }

    /** Verifica il pagamento di una prenotazione parcheggiata. */
    @Test
    void pagaPrenotazione_ok() {
        prenotazione.setStato(StatoPrenotazione.parcheggiato);
        configuraAnalitica("p1");

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response =
                service.pagaPrenotazione("1", 10.0);

        assertEquals(StatoPrenotazione.pagato, response.stato());
        assertEquals(10.0, response.importoPagato());
        assertEquals(now, prenotazione.getDataPagamento());
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica il rifiuto del pagamento in stato non valido. */
    @Test
    void pagaPrenotazione_statoNonValido() {
        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                IllegalStateException.class,
                () -> service.pagaPrenotazione("1", 10.0));
    }

    /** Verifica il rifiuto di un importo non positivo. */
    @Test
    void pagaPrenotazione_importoNonValido() {
        prenotazione.setStato(StatoPrenotazione.parcheggiato);
        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.pagaPrenotazione("1", 0.0));
    }

    /** Verifica la conclusione della prenotazione e la liberazione del posto. */
    @Test
    void validaUscita_ok() {
        prenotazione.setStato(StatoPrenotazione.pagato);
        prenotazione.setDataPagamento(now.minusMinutes(5));
        prenotazione.setImportoPagato(10.0);
        prenotazione.setPosto(creaSnapshot(
                "1-01", StatoPosto.OCCUPATO));
        configuraAnalitica("p1");

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response = service.validaUscita("QR1");

        assertEquals(StatoPrenotazione.conclusa, response.stato());
        assertEquals(now, response.dataUscita());
        assertEquals(StatoPosto.LIBERO, response.posto().getStato());
        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.LIBERO);
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica l'aggiunta della fee quando il tempo di uscita è scaduto. */
    @Test
    void validaUscita_scaduta() {
        prenotazione.setStato(StatoPrenotazione.pagato);
        prenotazione.setDataPagamento(now.minusMinutes(20));
        prenotazione.setImportoPagato(10.0);

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
                IllegalStateException.class,
                () -> service.validaUscita("QR1"));

        assertEquals(15.0, prenotazione.getImportoPagato(), 0.01);
        verify(postoService, never()).aggiornaStato(any(), any(), any());
    }

    /** Verifica il rifiuto dell'uscita prima del pagamento. */
    @Test
    void validaUscita_nonPagato() {
        prenotazione.setStato(StatoPrenotazione.inCorso);
        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                IllegalStateException.class,
                () -> service.validaUscita("QR1"));
    }

    /** Verifica il recupero di una prenotazione tramite codice QR. */
    @Test
    void getPrenotazioneByQr_ok() {
        prenotazione.setPosto(creaSnapshot(
                "1-01", StatoPosto.PRENOTATO));
        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        PrenotazioneResponse response =
                service.getPrenotazioneByQr("QR1");

        assertEquals("1", response.id());
        assertEquals("QR1", response.codiceQr());
        assertNotNull(response.posto());
        assertEquals("1-01", response.posto().getSlotId());
    }

    /** Verifica l'errore per un QR non associato a prenotazioni. */
    @Test
    void getPrenotazioneByQr_notFound() {
        when(prenotazioneRepository.findByCodiceQr("QR404"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.getPrenotazioneByQr("QR404"));

        assertTrue(exception.getMessage().contains(
                "Prenotazione non trovata"));
    }

    /** Verifica la conferma del parcheggio e lo stato OCCUPATO dello slot. */
    @Test
    void confermaParcheggio_ok() {
        prenotazione.setStato(StatoPrenotazione.inCorso);
        prenotazione.setPosto(creaSnapshot(
                "1-01", StatoPosto.PRENOTATO));
        configuraAnalitica("p1");

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(prenotazioneRepository.save(any(Prenotazione.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrenotazioneResponse response =
                service.confermaParcheggio("1");

        assertEquals(StatoPrenotazione.parcheggiato, response.stato());
        assertEquals(StatoPosto.OCCUPATO, response.posto().getStato());
        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.OCCUPATO);
        verify(logService).salvaLog(any(LogRequest.class));
    }

    /** Verifica l'idempotenza della conferma già eseguita. */
    @Test
    void confermaParcheggio_giaParcheggiato() {
        prenotazione.setStato(StatoPrenotazione.parcheggiato);
        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        PrenotazioneResponse response =
                service.confermaParcheggio("1");

        assertEquals(StatoPrenotazione.parcheggiato, response.stato());
        verify(postoService, never()).aggiornaStato(any(), any(), any());
        verify(prenotazioneRepository, never()).save(any());
    }

    /** Verifica il rifiuto della conferma da uno stato non ammesso. */
    @Test
    void confermaParcheggio_statoNonValido() {
        prenotazione.setStato(StatoPrenotazione.pagato);
        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(
                IllegalStateException.class,
                () -> service.confermaParcheggio("1"));
    }

    /** Configura un'analitica valida per la generazione dei log. */
    private void configuraAnalitica(String parcheggioId) {
        Analitiche analitica = org.mockito.Mockito.mock(Analitiche.class);
        when(analitica.getId()).thenReturn("a1");
        when(analiticheRepository.findByParcheggioId(parcheggioId))
                .thenReturn(Optional.of(analitica));
    }

    /** Crea lo snapshot del posto memorizzato nella prenotazione. */
    private PostoResponse creaSnapshot(
            String slotId,
            StatoPosto stato) {
        Posto posto = new Posto(
                slotId,
                1,
                "P1-01",
                1,
                TipoPosto.NORMALE,
                stato,
                false,
                1);
        return new PostoResponse(posto, "p1");
    }
}
