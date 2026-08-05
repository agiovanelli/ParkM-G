package pmg.backend.prenotazione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogService;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.posto.PostoService;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;

/**
 * Verifica il calcolo dell'importo senza collegarsi a MongoDB.
 *
 * Tutti i repository sono mock Mockito. Nessun test esegue operazioni di
 * scrittura o cancellazione su collezioni reali. Il nome della classe è
 * mantenuto per consentire la sostituzione diretta del precedente file.
 */
@ExtendWith(MockitoExtension.class)
class CalcolaImportoIntegrationTest {

    /** Repository prenotazioni simulato. */
    @Mock
    private PrenotazioneRepository prenotazioneRepository;

    /** Repository utenti simulato. */
    @Mock
    private UtenteRepository utenteRepository;

    /** Repository parcheggi simulato. */
    @Mock
    private ParcheggioRepository parcheggioRepository;

    /** Servizio log simulato. */
    @Mock
    private LogService logService;

    /** Repository analitiche simulato. */
    @Mock
    private AnaliticheRepository analiticheRepository;

    /** Servizio posti simulato. */
    @Mock
    private PostoService postoService;

    /** Servizio sottoposto a test. */
    @InjectMocks
    private PrenotazioneServiceImpl prenotazioneService;

    /** Clock fisso usato per rendere deterministici i test temporali. */
    private Clock fixedClock;

    /** Data e ora corrente secondo il clock di test. */
    private LocalDateTime now;

    /** Configura esclusivamente il clock; non modifica alcun dato MongoDB. */
    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(
                LocalDateTime.of(2024, 1, 15, 10, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault());

        prenotazioneService.setClock(fixedClock);
        now = LocalDateTime.now(fixedClock);
    }

    /** Verifica che una prenotazione senza ingresso abbia importo nullo. */
    @Test
    void calcolaImporto_senzaIngresso_returnZero() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataCreazione(now.minusMinutes(5));
        preparaPrenotazione(prenotazione);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(0.0, result, 0.001);
    }

    /** Verifica la tariffa base di tre euro per un'ora. */
    @Test
    void calcolaImporto_base_unOra() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }

    /** Verifica l'applicazione combinata degli sconti giovane e studente. */
    @Test
    void calcolaImporto_scontoUtente() {
        Utente utente = creaUtente(Map.of(
                "eta", "under30",
                "occupazione", "studente"));

        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, utente, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(2.30, result, 0.01);
    }

    /** Verifica lo sconto previsto per gli utenti over 60. */
    @Test
    void calcolaImporto_over60() {
        Utente utente = creaUtente(Map.of("eta", "over60"));

        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, utente, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(2.4, result, 0.01);
    }

    /** Verifica il sovrapprezzo notturno. */
    @Test
    void calcolaImporto_notturno() {
        Clock nightClock = Clock.fixed(
                LocalDateTime.of(2024, 1, 15, 2, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault());
        prenotazioneService.setClock(nightClock);
        LocalDateTime nightNow = LocalDateTime.now(nightClock);

        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(nightNow.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.3, result, 0.01);
    }

    /** Verifica il sovrapprezzo del fine settimana. */
    @Test
    void calcolaImporto_weekend() {
        Clock saturdayClock = Clock.fixed(
                LocalDateTime.of(2024, 1, 13, 10, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault());
        prenotazioneService.setClock(saturdayClock);
        LocalDateTime saturdayNow = LocalDateTime.now(saturdayClock);

        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(saturdayNow.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.6, result, 0.01);
    }

    /** Verifica la fee con occupazione media del parcheggio. */
    @Test
    void calcolaImporto_feeMedia() {
        Parcheggio parcheggio = creaParcheggio(100, 40);
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(20));
        preparaContesto(prenotazione, null, parcheggio);

        double result = prenotazioneService.calcolaImporto("P1");

        assertTrue(result > 3.0);
    }

    /** Verifica la fee con occupazione elevata del parcheggio. */
    @Test
    void calcolaImporto_feeAlta() {
        Parcheggio parcheggio = creaParcheggio(100, 10);
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(20));
        preparaContesto(prenotazione, null, parcheggio);

        double result = prenotazioneService.calcolaImporto("P1");

        assertTrue(result > 3.0);
    }

    /** Verifica l'assenza di fee con bassa occupazione. */
    @Test
    void calcolaImporto_noFee() {
        Parcheggio parcheggio = creaParcheggio(100, 90);
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, parcheggio);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }

    /** Verifica la penale per un ingresso con ritardo superiore a dieci minuti. */
    @Test
    void calcolaImporto_penaleRitardo() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(30));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertTrue(result > 3.0);
    }

    /** Verifica lo sconto per una permanenza di almeno dodici ore. */
    @Test
    void calcolaImporto_scontoLungaDurata() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(13));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(29.25, result, 0.01);
    }

    /** Verifica che la durata minima venga arrotondata a un'ora. */
    @Test
    void calcolaImporto_durataMinima() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusSeconds(10));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(1));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }

    /** Verifica il calcolo con un utente privo di preferenze. */
    @Test
    void calcolaImporto_utenteSenzaPreferenze() {
        Utente utente = creaUtente(null);
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(1));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, utente, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }

    /** Verifica lo sconto del cinquanta per cento oltre le ventiquattro ore. */
    @Test
    void calcolaImporto_sconto24h() {
        Prenotazione prenotazione = creaPrenotazioneBase();
        prenotazione.setDataIngresso(now.minusHours(25));
        prenotazione.setDataCreazione(
                prenotazione.getDataIngresso().minusMinutes(5));
        preparaContesto(prenotazione, null, null);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(37.5, result, 0.01);
    }

    /** Verifica l'errore quando la prenotazione non esiste. */
    @Test
    void calcolaImporto_prenotazioneNonEsiste() {
        when(prenotazioneRepository.findById("NOT_FOUND"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> prenotazioneService.calcolaImporto("NOT_FOUND"));
    }

    /** Configura il repository simulato per la prenotazione. */
    private void preparaPrenotazione(Prenotazione prenotazione) {
        when(prenotazioneRepository.findById(prenotazione.getId()))
                .thenReturn(Optional.of(prenotazione));
    }

    /** Configura tutti i dati simulati necessari al calcolo. */
    private void preparaContesto(
            Prenotazione prenotazione,
            Utente utente,
            Parcheggio parcheggio) {
        preparaPrenotazione(prenotazione);

        when(utenteRepository.findById(prenotazione.getUtenteId()))
                .thenReturn(Optional.ofNullable(utente));
        when(parcheggioRepository.findById(prenotazione.getParcheggioId()))
                .thenReturn(Optional.ofNullable(parcheggio));
    }

    /** Crea una prenotazione con gli identificativi comuni ai test. */
    private Prenotazione creaPrenotazioneBase() {
        Prenotazione prenotazione = new Prenotazione();
        prenotazione.setId("P1");
        prenotazione.setUtenteId("U1");
        prenotazione.setParcheggioId("PK1");
        return prenotazione;
    }

    /** Crea un utente sintetico con le preferenze indicate. */
    private Utente creaUtente(Map<String, String> preferenze) {
        Utente utente = new Utente();
        utente.setId("U1");
        utente.setPreferenze(preferenze);
        return utente;
    }

    /** Crea un parcheggio sintetico con i contatori indicati. */
    private Parcheggio creaParcheggio(
            int postiTotali,
            int postiDisponibili) {
        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setId("PK1");
        parcheggio.setPostiTotali(postiTotali);
        parcheggio.setPostiDisponibili(postiDisponibili);
        return parcheggio;
    }
}
