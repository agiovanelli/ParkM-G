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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.springframework.test.util.ReflectionTestUtils;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.maps.MapsServiceImpl;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import pmg.backend.utente.UtenteService;

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
    private PostoRepository postoRepository;

    @Mock
    private MapsServiceImpl mapsService;

    @InjectMocks
    private ParcheggioServiceImpl service;

    @Test
    void cercaPerAreaTest() {
        Parcheggio p1 = new Parcheggio("A", "Centro", 100, 50, 45.5, 9.1);
        p1.setId("1");
        Parcheggio p2 = new Parcheggio("B", "Centro", 80, 20, 45.6, 9.2);
        p2.setId("2");

        when(parcheggioRepository.findByAreaContainingIgnoreCase("Centro")).thenReturn(List.of(p1, p2));

        List<ParcheggioResponse> result = service.cercaPerArea("Centro");

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals("A", result.get(0).nome());
        assertEquals("2", result.get(1).id());

        verify(parcheggioRepository).findByAreaContainingIgnoreCase("Centro");
        verifyNoMoreInteractions(parcheggioRepository);
    }

    @Test
    void effettuaPrenotazioneTest() {
        Parcheggio parcheggio = new Parcheggio("A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p1");

        Utente utente = org.mockito.Mockito.mock(Utente.class);
        when(utente.getId()).thenReturn("u1");

        Posto posto = org.mockito.Mockito.mock(Posto.class);
        when(posto.isDisponibile()).thenReturn(true);
        when(posto.isDisabilitato()).thenReturn(false);
        when(posto.isRiservatoDisabili()).thenReturn(false);
        when(posto.isRiservatoIncinta()).thenReturn(false);
        when(posto.getDistanzaUscita()).thenReturn(1);

        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));
        when(utenteRepository.findById("u1")).thenReturn(Optional.of(utente));
        when(utenteService.getPreferenze("u1")).thenReturn(Map.of("distanza", "1"));
        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1")).thenReturn(List.of(posto));
        when(postoRepository.save(any(Posto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(parcheggioRepository.save(any(Parcheggio.class))).thenAnswer(inv -> inv.getArgument(0));

        when(prenotazioneRepository.save(any(Prenotazione.class))).thenAnswer(inv -> {
            Prenotazione p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", "pr1");
            return p;
        });

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "p1", LocalDateTime.of(2025, 1, 1, 10, 0), null, null);

        PrenotazioneResponse resp = service.effettuaPrenotazione(req);

        assertNotNull(resp);
        assertEquals("pr1", resp.id());
        assertEquals("u1", resp.utenteId());
        assertEquals("p1", resp.parcheggioId());
        assertNotNull(resp.codiceQr());
        assertNotNull(resp.posto());
        assertNotNull(resp.scadenzaArrivo());
        assertEquals(9, parcheggio.getPostiDisponibili());

        verify(parcheggioRepository).findById("p1");
        verify(utenteRepository).findById("u1");
        verify(utenteService).getPreferenze("u1");
        verify(postoRepository).findByParcheggioIdOrderByPianoAscNumeroAsc("p1");
        verify(postoRepository).save(any(Posto.class));
        verify(parcheggioRepository).save(any(Parcheggio.class));
        verify(prenotazioneRepository).save(any(Prenotazione.class));
    }

    @Test
    void effettuaPrenotazioneParcheggioInEmergenzaTest() {
        Parcheggio parcheggio = new Parcheggio("A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p1");
        parcheggio.setInEmergenza(true);

        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "p1", LocalDateTime.now(), null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.effettuaPrenotazione(req));
        assertEquals("Parcheggio in emergenza", ex.getMessage());

        verify(parcheggioRepository).findById("p1");
        verify(utenteRepository, never()).findById(any());
        verify(prenotazioneRepository, never()).save(any());
    }

    @Test
    void effettuaPrenotazionePostiEsauritiTest() {
        Parcheggio parcheggio = new Parcheggio("A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p2");

        Utente utente = org.mockito.Mockito.mock(Utente.class);
        when(utente.getId()).thenReturn("u1");

        when(parcheggioRepository.findById("p2")).thenReturn(Optional.of(parcheggio));
        when(utenteRepository.findById("u1")).thenReturn(Optional.of(utente));
        when(utenteService.getPreferenze("u1")).thenReturn(Map.of("distanza", "1"));
        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p2")).thenReturn(List.of());

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "p2", LocalDateTime.now(), null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.effettuaPrenotazione(req));
        assertEquals("Posti esauriti", ex.getMessage());

        verify(parcheggioRepository).findById("p2");
        verify(utenteRepository).findById("u1");
        verify(utenteService).getPreferenze("u1");
        verify(postoRepository).findByParcheggioIdOrderByPianoAscNumeroAsc("p2");
        verify(postoRepository, never()).save(any());
        verify(prenotazioneRepository, never()).save(any());
    }

    @Test
    void effettuaPrenotazioneParcheggioNonTrovatoTest() {
        when(parcheggioRepository.findById("missing")).thenReturn(Optional.empty());

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "missing", LocalDateTime.now(), null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.effettuaPrenotazione(req));
        assertEquals("Parcheggio non trovato", ex.getMessage());

        verify(parcheggioRepository).findById("missing");
        verify(prenotazioneRepository, never()).save(any());
    }

    @Test
    void cercaViciniTest() {
        Parcheggio vicino = new Parcheggio("Vicino", "Centro", 50, 10, 45.50, 9.20);
        vicino.setId("v1");

        Parcheggio lontano = new Parcheggio("Lontano", "Centro", 50, 10, 46.00, 10.00);
        lontano.setId("l1");

        when(parcheggioRepository.findAll()).thenReturn(List.of(vicino, lontano));

        List<ParcheggioResponse> result = service.cercaVicini(45.50, 9.20, 500.0);

        assertEquals(1, result.size());
        assertEquals("v1", result.get(0).id());
        assertEquals("Vicino", result.get(0).nome());

        verify(parcheggioRepository).findAll();
    }

    @Test
    void impostaStatoEmergenzaAttivaTest() {
        Parcheggio parcheggio = new Parcheggio("Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        parcheggio.setId("p1");

        Analitiche analitica = new Analitiche("p1", "Parcheggio A", "op1");
        ReflectionTestUtils.setField(analitica, "id", "a1");

        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(analiticheRepository.findByParcheggioId("p1")).thenReturn(Optional.of(analitica));

        service.impostaStatoEmergenza("p1", true, "Incendio");

        assertTrue(parcheggio.isInEmergenza());

        verify(parcheggioRepository).findById("p1");
        verify(parcheggioRepository).save(parcheggio);
        verify(analiticheRepository).findByParcheggioId("p1");
        verify(logService).salvaLog(any(LogRequest.class));
    }

    @Test
    void impostaStatoEmergenzaDisattivaTest() {
        Parcheggio parcheggio = new Parcheggio("Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        parcheggio.setId("p1");
        parcheggio.setInEmergenza(true);

        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.impostaStatoEmergenza("p1", false, null);

        assertFalse(parcheggio.isInEmergenza());

        verify(parcheggioRepository).findById("p1");
        verify(parcheggioRepository).save(parcheggio);
        verify(logService, never()).salvaLog(any(LogRequest.class));
    }

    @Test
    void assegnaPostoOttimaleTest() {
        Posto postoVicino = org.mockito.Mockito.mock(Posto.class);
        when(postoVicino.isDisponibile()).thenReturn(true);
        when(postoVicino.isDisabilitato()).thenReturn(false);
        when(postoVicino.isRiservatoDisabili()).thenReturn(false);
        when(postoVicino.isRiservatoIncinta()).thenReturn(false);
        when(postoVicino.getDistanzaUscita()).thenReturn(1);

        Posto postoLontano = org.mockito.Mockito.mock(Posto.class);
        when(postoLontano.isDisponibile()).thenReturn(true);
        when(postoLontano.isDisabilitato()).thenReturn(false);
        when(postoLontano.isRiservatoDisabili()).thenReturn(false);
        when(postoLontano.isRiservatoIncinta()).thenReturn(false);
        when(postoLontano.getDistanzaUscita()).thenReturn(4);

        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of(postoLontano, postoVicino));

        Posto result = service.assegnaPostoOttimale("p1", Map.of("distanza", "1"));

        assertNotNull(result);
        assertEquals(postoVicino, result);

        verify(postoRepository).findByParcheggioIdOrderByPianoAscNumeroAsc("p1");
    }

    @Test
    void assegnaPostoOttimaleNessunPostoTest() {
        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of());

        Posto result = service.assegnaPostoOttimale("p1", Map.of("distanza", "1"));

        assertNull(result);

        verify(postoRepository).findByParcheggioIdOrderByPianoAscNumeroAsc("p1");
    }

    @Test
    void getByIdTest() {
        Parcheggio p = new Parcheggio("Parcheggio A", "Centro", 100, 20, 45.5, 9.1);
        p.setId("p1");

        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(p));

        ParcheggioResponse response = service.getById("p1");

        assertEquals("p1", response.id());
        assertEquals("Parcheggio A", response.nome());
        assertEquals("Centro", response.area());

        verify(parcheggioRepository).findById("p1");
    }

    @Test
    void getByIdNonTrovatoTest() {
        when(parcheggioRepository.findById("missing")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getById("missing"));
        assertEquals("Parcheggio non trovato", ex.getMessage());

        verify(parcheggioRepository).findById("missing");
    }

    @Test
    void getPostiSenzaPianoTest() {
        Posto posto = org.mockito.Mockito.mock(Posto.class);
        when(posto.getId()).thenReturn("posto-1");
        when(posto.getNumero()).thenReturn(10);
        when(posto.getPiano()).thenReturn(1);
        when(posto.isDisponibile()).thenReturn(true);
        when(posto.isDisabilitato()).thenReturn(false);
        when(posto.isRiservatoDisabili()).thenReturn(false);
        when(posto.isRiservatoIncinta()).thenReturn(false);
        when(posto.getParcheggioId()).thenReturn("p1");
        when(posto.getDistanzaUscita()).thenReturn(2);

        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of(posto));

        List<PostoResponse> result = service.getPosti("p1", null);

        assertEquals(1, result.size());

        verify(postoRepository).findByParcheggioIdOrderByPianoAscNumeroAsc("p1");
    }

    @Test
    void getPostiConPianoTest() {
        Posto posto = org.mockito.Mockito.mock(Posto.class);
        when(posto.getId()).thenReturn("posto-2");
        when(posto.getNumero()).thenReturn(5);
        when(posto.getPiano()).thenReturn(2);
        when(posto.isDisponibile()).thenReturn(true);
        when(posto.isDisabilitato()).thenReturn(false);
        when(posto.isRiservatoDisabili()).thenReturn(false);
        when(posto.isRiservatoIncinta()).thenReturn(false);
        when(posto.getParcheggioId()).thenReturn("p1");
        when(posto.getDistanzaUscita()).thenReturn(1);

        when(postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc("p1", 2))
                .thenReturn(List.of(posto));

        List<PostoResponse> result = service.getPosti("p1", 2);

        assertEquals(1, result.size());

        verify(postoRepository).findByParcheggioIdAndPianoOrderByNumeroAsc("p1", 2);
    }

    @Test
    void syncPostiStatsTest() {
        Parcheggio parcheggio = new Parcheggio("Parcheggio A", "Centro", 0, 0, 45.5, 9.1);
        parcheggio.setId("p1");

        when(postoRepository.countByParcheggioId("p1")).thenReturn((int) 120L);
        when(postoRepository.countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse("p1")).thenReturn((int) 35L);
        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.syncPostiStats("p1");

        assertEquals(120, parcheggio.getPostiTotali());
        assertEquals(35, parcheggio.getPostiDisponibili());

        verify(postoRepository).countByParcheggioId("p1");
        verify(postoRepository).countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse("p1");
        verify(parcheggioRepository).findById("p1");
        verify(parcheggioRepository).save(parcheggio);
    }

    @Test
    void syncPostiStatsParcheggioNonTrovatoTest() {
        when(postoRepository.countByParcheggioId("p1")).thenReturn((int) 120L);
        when(postoRepository.countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse("p1")).thenReturn((int) 35L);
        when(parcheggioRepository.findById("p1")).thenReturn(Optional.empty());

        service.syncPostiStats("p1");

        verify(postoRepository).countByParcheggioId("p1");
        verify(postoRepository).countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse("p1");
        verify(parcheggioRepository).findById("p1");
        verify(parcheggioRepository, never()).save(any());
    }
}