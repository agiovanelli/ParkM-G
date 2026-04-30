package pmg.backend.prenotazione;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogService;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;
import pmg.backend.posto.PostoResponse;
import pmg.backend.utente.UtenteRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceImplTest {

    @Mock private PrenotazioneRepository prenotazioneRepository;
    @Mock private ParcheggioRepository parcheggioRepository;
    @Mock private UtenteRepository utenteRepository;
    @Mock private LogService logService;
    @Mock private AnaliticheRepository analiticheRepository;
    @Mock private PostoRepository postoRepository;
    
    @InjectMocks
    private PrenotazioneServiceImpl service;

    private Prenotazione prenotazione;

    @BeforeEach
    void setup() {
        prenotazione = new Prenotazione();
        prenotazione.setId("1");
        prenotazione.setUtenteId("u1");
        prenotazione.setParcheggioId("p1");
        prenotazione.setCodiceQr("QR1");
        prenotazione.setDataCreazione(LocalDateTime.now());
        prenotazione.setStato(StatoPrenotazione.attiva);
    }

    @Test
    void getStoricoUtente_ok() {
        when(prenotazioneRepository.findByUtenteId("u1"))
                .thenReturn(List.of(prenotazione));

        List<PrenotazioneResponse> res = service.getStoricoUtente("u1");

        assertEquals(1, res.size());
        assertEquals("u1", res.get(0).utenteId());
    }
    
    @Test
    void validaIngresso_ok() {
        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class, () -> {
            when(analiticheRepository.findByParcheggioId(any()))
                    .thenReturn(Optional.empty());
            service.validaIngresso("QR1");
        });
    }
    
    @Test
    void validaIngresso_statoNonValido() {
        prenotazione.setStato(StatoPrenotazione.scaduta);

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(Exception.class,
                () -> service.validaIngresso("QR1"));
    }
    
    @Test
    void annullaPrenotazione_ok() {

        prenotazione.setStato(StatoPrenotazione.attiva);

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));

        when(parcheggioRepository.findById(any()))
                .thenReturn(Optional.of(mock(pmg.backend.parcheggio.Parcheggio.class)));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(analiticheRepository.findByParcheggioId(any()))
                .thenReturn(Optional.of(mock(pmg.backend.analitiche.Analitiche.class)));

        PrenotazioneResponse res = service.annullaPrenotazione("1", "u1");

        assertEquals(StatoPrenotazione.annullata, res.stato());
    }
    
    @Test
    void calcolaImporto_base() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_conSconto() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));

        Map<String, String> prefs = new HashMap<>();
        prefs.put("eta", "under30");

        var utente = mock(pmg.backend.utente.Utente.class);
        when(utente.getPreferenze()).thenReturn(prefs);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(utenteRepository.findById("u1"))
                .thenReturn(Optional.of(utente));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void pagaPrenotazione_ok() {

        prenotazione.setStato(StatoPrenotazione.parcheggiato);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(analiticheRepository.findByParcheggioId(any()))
                .thenReturn(Optional.of(mock(pmg.backend.analitiche.Analitiche.class)));

        PrenotazioneResponse res = service.pagaPrenotazione("1", 10.0);

        assertEquals(StatoPrenotazione.pagato, res.stato());
    }
    
    @Test
    void validaUscita_ok() {

        Prenotazione p = new Prenotazione();
        p.setId("1");
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.pagato);
        p.setDataPagamento(LocalDateTime.now().minusMinutes(5));

        Posto posto = new Posto();
        posto.setId("posto1");
        posto.setDisponibile(false);
        p.setPosto(new PostoResponse(posto));

        Posto postoDb = new Posto();
        postoDb.setId("posto1");
        postoDb.setDisponibile(false);

        Parcheggio park = mock(Parcheggio.class);
        when(park.getPostiTotali()).thenReturn(100);
        when(park.getPostiDisponibili()).thenReturn(50);

        Analitiche analitica = mock(Analitiche.class);
        when(analitica.getId()).thenReturn("a1");

        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.of(p));

        when(postoRepository.findByIdAndParcheggioId("posto1", "p1"))
                .thenReturn(Optional.of(postoDb));

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));

        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        PrenotazioneResponse res = service.validaUscita("QR");

        assertEquals(StatoPrenotazione.conclusa, p.getStato());
        assertTrue(postoDb.isDisponibile());
        verify(logService).salvaLog(any());
        assertNotNull(res);
    }
       
    @Test
    void controllaPrenotazioniScadute_postoLiberato() {

        Prenotazione p = new Prenotazione();
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.attiva);

        Posto postoPren = new Posto();
        postoPren.setId("posto1");
        p.setPosto(new PostoResponse(postoPren));

        Posto postoDb = new Posto();
        postoDb.setId("posto1");
        postoDb.setDisponibile(false);

        when(prenotazioneRepository.findByStatoAndDataCreazioneBefore(any(), any()))
                .thenReturn(List.of(p));

        when(postoRepository.findByIdAndParcheggioId("posto1", "p1"))
                .thenReturn(Optional.of(postoDb));

        var park = mock(pmg.backend.parcheggio.Parcheggio.class);
        when(park.getPostiTotali()).thenReturn(100);
        when(park.getPostiDisponibili()).thenReturn(50);

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));

        service.controllaPrenotazioniScadute();

        verify(postoRepository).save(any());
        verify(parcheggioRepository).save(any());
    }
    
    @Test
    void controllaPrenotazioniScadute_postoGiaDisponibile() {

        Prenotazione p = new Prenotazione();
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.attiva);

        Posto postoPren = new Posto();
        postoPren.setId("posto1");
        p.setPosto(new PostoResponse(postoPren));

        Posto postoDb = new Posto();
        postoDb.setDisponibile(true);

        when(prenotazioneRepository.findByStatoAndDataCreazioneBefore(any(), any()))
                .thenReturn(List.of(p));

        when(postoRepository.findByIdAndParcheggioId(any(), any()))
                .thenReturn(Optional.of(postoDb));

        service.controllaPrenotazioniScadute();

        verify(parcheggioRepository, never()).save(any());
    }
    
    @Test
    void controllaPrenotazioniScadute_senzaPosto() {

        Prenotazione p = new Prenotazione();
        p.setStato(StatoPrenotazione.attiva);
        p.setParcheggioId("p1");

        when(prenotazioneRepository.findByStatoAndDataCreazioneBefore(any(), any()))
                .thenReturn(List.of(p));

        service.controllaPrenotazioniScadute();

        verify(postoRepository, never()).save(any());
    }
    
    @Test
    void annullaPrenotazione_statoNonValidoPagato() {

        prenotazione.setStato(StatoPrenotazione.pagato);

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(IllegalStateException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }
    
    @Test
    void annullaPrenotazione_statoNonValidoConclusa() {
        Prenotazione p = new Prenotazione();
        p.setStato(StatoPrenotazione.conclusa);

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }
    
    @Test
    void annullaPrenotazione_postoNonTrovato() {

        Posto posto = new Posto();
        posto.setId("p1");
        prenotazione.setPosto(new PostoResponse(posto));
        prenotazione.setStato(StatoPrenotazione.attiva);

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(prenotazione));

        when(postoRepository.findByIdAndParcheggioId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }
    
    @Test
    void calcolaImporto_noIngresso() {

        prenotazione.setDataIngresso(null);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertEquals(0.0, result);
    }
    
    @Test
    void calcolaImporto_durataMinima() {

        prenotazione.setDataIngresso(LocalDateTime.now());

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_weekendNotte() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    
    
    @Test
    void calcolaImporto_feeAltaOccupazione() {

        prenotazione.setDataCreazione(LocalDateTime.now().minusHours(3)); // PRIMA
        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));  // DOPO

        var park = mock(pmg.backend.parcheggio.Parcheggio.class);
        when(park.getPostiTotali()).thenReturn(100);
        when(park.getPostiDisponibili()).thenReturn(10); // 90%

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_penaleRitardo() {

        prenotazione.setDataCreazione(LocalDateTime.now().minusMinutes(30));
        prenotazione.setDataIngresso(LocalDateTime.now().minusMinutes(5));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_lungaDurata() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(30));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void validaUscita_nonPagato() {

        prenotazione.setStato(StatoPrenotazione.inCorso);

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(IllegalStateException.class,
                () -> service.validaUscita("QR1"));
    }
    
    @Test
    void validaUscita_scaduta() {
        Prenotazione p = new Prenotazione();
        p.setStato(StatoPrenotazione.pagato);
        p.setDataPagamento(LocalDateTime.now().minusMinutes(20)); // scaduta

        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.validaUscita("QR"));

        assertTrue(p.getImportoPagato() > 0); // fee applicata
    }
    
    @Test
    void confermaParcheggio_giaParcheggiato() {

        prenotazione.setStato(StatoPrenotazione.parcheggiato);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        PrenotazioneResponse res = service.confermaParcheggio("1");

        assertEquals(StatoPrenotazione.parcheggiato, res.stato());
    }
    
    @Test
    void confermaParcheggio_statoNonValido() {

        prenotazione.setStato(StatoPrenotazione.pagato);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(IllegalStateException.class,
                () -> service.confermaParcheggio("1"));
    }
    
    @Test
    void salvaLogEvento_ok() {

        var analitica = mock(pmg.backend.analitiche.Analitiche.class);
        when(analitica.getId()).thenReturn("a1");

        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));

        prenotazione.setStato(StatoPrenotazione.attiva);

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        service.validaIngresso("QR1");

        verify(logService).salvaLog(any());
    }
    
    @Test
    void salvaLogEvento_analiticaNotFound() {

        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.empty());

        prenotazione.setStato(StatoPrenotazione.attiva);

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class,
                () -> service.validaIngresso("QR1"));
    }
    
    @Test
    void getByParcheggio_ok() {

        when(prenotazioneRepository.findByParcheggioId("p1"))
                .thenReturn(List.of(prenotazione));

        List<PrenotazioneResponse> res = service.getByParcheggio("p1");

        assertEquals(1, res.size());
    }
    
    @Test
    void feePermanenza_ok() {

        LocalDateTime scadenza = LocalDateTime.now().minusMinutes(10);

        double result = service.feePermanenza(scadenza);

        assertTrue(result >= 10);
    }
    
    @Test
    void pagaPrenotazione_statoNonValido() {

        prenotazione.setStato(StatoPrenotazione.attiva);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        assertThrows(IllegalStateException.class,
                () -> service.pagaPrenotazione("1", 10));
    }
    
    @Test
    void validaUscita_postoNonTrovato() {

        prenotazione.setStato(StatoPrenotazione.pagato);
        prenotazione.setDataPagamento(LocalDateTime.now());

        Posto posto = new Posto();
        posto.setId("p1");
        prenotazione.setPosto(new PostoResponse(posto));

        when(prenotazioneRepository.findByCodiceQr("QR1"))
                .thenReturn(Optional.of(prenotazione));

        when(postoRepository.findByIdAndParcheggioId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.validaUscita("QR1"));
    }
    
    @Test
    void controllaPrenotazioniScadute_postoNullDaRepo() {

        Prenotazione p = new Prenotazione();
        p.setParcheggioId("p1");

        Posto posto = new Posto();
        posto.setId("p1");
        p.setPosto(new PostoResponse(posto));

        when(prenotazioneRepository.findByStatoAndDataCreazioneBefore(any(), any()))
                .thenReturn(List.of(p));

        when(postoRepository.findByIdAndParcheggioId(any(), any()))
                .thenReturn(Optional.empty());

        service.controllaPrenotazioniScadute();

        verify(parcheggioRepository, never()).save(any());
    }
    
    @Test
    void calcolaImporto_feeMediaOccupazione() {

        LocalDateTime base = LocalDateTime.now();

        prenotazione.setDataCreazione(base.minusHours(3));
        prenotazione.setDataIngresso(base.minusHours(2));

        var park = mock(pmg.backend.parcheggio.Parcheggio.class);
        when(park.getPostiTotali()).thenReturn(100);
        when(park.getPostiDisponibili()).thenReturn(40); // 60%

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_utenteNull() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        when(utenteRepository.findById(any()))
                .thenReturn(Optional.empty());

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void calcolaImporto_parcheggioNull() {

        prenotazione.setDataIngresso(LocalDateTime.now().minusHours(2));

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        when(parcheggioRepository.findById(any()))
                .thenReturn(Optional.empty());

        double result = service.calcolaImporto("1");

        assertTrue(result > 0);
    }
    
    @Test
    void confermaParcheggio_ok() {

        prenotazione.setStato(StatoPrenotazione.inCorso);

        when(prenotazioneRepository.findById("1"))
                .thenReturn(Optional.of(prenotazione));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var analitica = mock(pmg.backend.analitiche.Analitiche.class);
        when(analitica.getId()).thenReturn("a1");

        when(analiticheRepository.findByParcheggioId(any()))
                .thenReturn(Optional.of(analitica));

        PrenotazioneResponse res = service.confermaParcheggio("1");

        assertEquals(StatoPrenotazione.parcheggiato, res.stato());
    }
    
    @Test
    void getPrenotazioneByQr_ok() {
        Prenotazione p = new Prenotazione();
        p.setId("1");
        p.setUtenteId("u1");
        p.setParcheggioId("p1");
        p.setCodiceQr("QR123");
        p.setStato(StatoPrenotazione.attiva);
        p.setDataCreazione(LocalDateTime.now());

        when(prenotazioneRepository.findByCodiceQr("QR123"))
                .thenReturn(Optional.of(p));

        PrenotazioneResponse res = service.getPrenotazioneByQr("QR123");

        assertNotNull(res);
        assertEquals("1", res.id());
        assertEquals("u1", res.utenteId());
        assertEquals("p1", res.parcheggioId());
        assertEquals("QR123", res.codiceQr());
    }
    
    @Test
    void getPrenotazioneByQr_notFound() {
        when(prenotazioneRepository.findByCodiceQr("QR404"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrenotazioneByQr("QR404"));

        assertTrue(ex.getMessage().contains("Prenotazione non trovata"));
    }
    
    @Test
    void getPrenotazioneByQr_fullMapping() {
        Prenotazione p = new Prenotazione();
        p.setId("99");
        p.setUtenteId("userX");
        p.setParcheggioId("parkX");
        p.setCodiceQr("QR999");
        p.setStato(StatoPrenotazione.parcheggiato);
        p.setDataCreazione(LocalDateTime.now().minusHours(1));
        p.setDataIngresso(LocalDateTime.now().minusMinutes(30));
        p.setDataUscita(LocalDateTime.now());
        p.setImportoPagato(12.5);

        Posto posto = new Posto();
        posto.setId("posto1");
        posto.setDisponibile(false);
        p.setPosto(new PostoResponse(posto));

        when(prenotazioneRepository.findByCodiceQr("QR999"))
                .thenReturn(Optional.of(p));

        PrenotazioneResponse res = service.getPrenotazioneByQr("QR999");

        assertEquals("99", res.id());
        assertEquals("userX", res.utenteId());
        assertEquals("parkX", res.parcheggioId());
        assertEquals("QR999", res.codiceQr());
        assertEquals(StatoPrenotazione.parcheggiato, res.stato());
        assertNotNull(res.dataIngresso());
        assertNotNull(res.dataUscita());
        assertEquals(12.5, res.importoPagato());
        assertNotNull(res.posto());
    }
    
    @Test
    void annullaPrenotazione_fullFlow() {
        Prenotazione p = new Prenotazione();
        p.setId("1");
        p.setUtenteId("u1");
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.attiva);

        Posto postoPren = new Posto();
        postoPren.setId("posto1");
        postoPren.setDisponibile(false);
        p.setPosto(new PostoResponse(postoPren));

        Posto postoDb = new Posto();
        postoDb.setId("posto1");
        postoDb.setDisponibile(false);

        Parcheggio park = mock(Parcheggio.class);
        when(park.getPostiTotali()).thenReturn(100);
        when(park.getPostiDisponibili()).thenReturn(10);

        Analitiche analitica = new Analitiche();
        analitica.setId("a1");

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(p));
        when(postoRepository.findByIdAndParcheggioId("posto1", "p1"))
                .thenReturn(Optional.of(postoDb));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));
        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));
        when(prenotazioneRepository.save(any()))
        .thenAnswer(inv -> inv.getArgument(0));

        PrenotazioneResponse res = service.annullaPrenotazione("1", "u1");

        assertEquals(StatoPrenotazione.annullata, p.getStato());
        assertTrue(postoDb.isDisponibile());
        verify(parcheggioRepository).save(any());
        verify(logService).salvaLog(any());
        assertNotNull(res);
    }
    
    @Test
    void annullaPrenotazione_notFound() {
        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.annullaPrenotazione("1", "u1"));
    }
    
    @Test
    void annullaPrenotazione_senzaPosto() {
        Prenotazione p = new Prenotazione();
        p.setId("1");
        p.setUtenteId("u1");
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.attiva);
        p.setPosto(null);
        
        Parcheggio park = new Parcheggio();
        park.setPostiTotali(50);
        park.setPostiDisponibili(20);

        Analitiche analitica = new Analitiche();
        analitica.setId("a1");

        when(prenotazioneRepository.findByIdAndUtenteId("1", "u1"))
                .thenReturn(Optional.of(p));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));
        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));
        when(prenotazioneRepository.save(any()))
        .thenAnswer(inv -> inv.getArgument(0));
        
        service.annullaPrenotazione("1", "u1");

        verify(postoRepository, never()).findByIdAndParcheggioId(any(), any());
    }
    
    @Test
    void validaUscita_qrNonValido() {
        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.validaUscita("QR"));
    }
    
    @Test
    void validaUscita_nonPagato_inCorso() {
        Prenotazione p = new Prenotazione();
        p.setStato(StatoPrenotazione.inCorso);

        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.validaUscita("QR"));
    }
    
    @Test
    void validaUscita_statoNonValido() {
        Prenotazione p = new Prenotazione();
        p.setStato(StatoPrenotazione.annullata);

        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.validaUscita("QR"));
    }
    
    @Test
    void validaUscita_senzaPosto() {
        Prenotazione p = new Prenotazione();
        p.setParcheggioId("p1");
        p.setStato(StatoPrenotazione.pagato);
        p.setDataPagamento(LocalDateTime.now());
        Parcheggio park = new Parcheggio();
        park.setPostiTotali(100);
        park.setPostiDisponibili(50);

        Analitiche analitica = new Analitiche();
        analitica.setId("a1");

        when(prenotazioneRepository.findByCodiceQr("QR"))
                .thenReturn(Optional.of(p));

        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(park));

        when(analiticheRepository.findByParcheggioId("p1"))
                .thenReturn(Optional.of(analitica));

        when(prenotazioneRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        PrenotazioneResponse res = service.validaUscita("QR");

        verify(postoRepository, never())
                .findByIdAndParcheggioId(any(), any());

        assertNotNull(res);
    }
}
