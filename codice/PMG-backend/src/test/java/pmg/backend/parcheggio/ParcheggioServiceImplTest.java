package pmg.backend.parcheggio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

@ExtendWith(MockitoExtension.class)
class ParcheggioServiceImplTest {

    @Mock
    private ParcheggioRepository parcheggioRepository;

    @Mock
    private PrenotazioneRepository prenotazioneRepository;

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
        assertEquals("A", result.get(0).nome());
        verify(parcheggioRepository).findByAreaContainingIgnoreCase("Centro");
    }

    @Test
    void effettuaPrenotazioneTest() {
        LocalDateTime dataCreazione = LocalDateTime.of(2025, 1, 1, 10, 0);

        Parcheggio parcheggio = new Parcheggio("A", "Centro", 100, 10, 45.5, 9.1);
        parcheggio.setId("p1");
        when(parcheggioRepository.findById("p1")).thenReturn(Optional.of(parcheggio));
        when(parcheggioRepository.save(any(Parcheggio.class))).thenAnswer(inv -> inv.getArgument(0));

        Prenotazione prenotazione = new Prenotazione("u1", "p1", dataCreazione, UUID.randomUUID().toString());
        prenotazione.setId("pr1");
        when(prenotazioneRepository.save(any(Prenotazione.class))).thenReturn(prenotazione);

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "p1", dataCreazione);
        PrenotazioneResponse resp = service.effettuaPrenotazione(req);

        assertEquals("pr1", resp.id());
        assertEquals("u1", resp.utenteId());
        assertEquals("p1", resp.parcheggioId());
        assertEquals(dataCreazione, resp.dataCreazione());

        verify(parcheggioRepository).findById("p1");
        verify(parcheggioRepository).save(any(Parcheggio.class));
        verify(prenotazioneRepository).save(any(Prenotazione.class));
    }

    @Test
    void effettuaPrenotazionePostiEsauritiTest() {
        LocalDateTime dataCreazione = LocalDateTime.of(2025, 1, 1, 11, 0);

        Parcheggio parcheggio = new Parcheggio("A", "Centro", 100, 0, 45.5, 9.1);
        parcheggio.setId("p2");
        when(parcheggioRepository.findById("p2")).thenReturn(Optional.of(parcheggio));

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "p2", dataCreazione);

        assertThrows(IllegalStateException.class, () -> service.effettuaPrenotazione(req));
        verify(parcheggioRepository).findById("p2");
        verify(parcheggioRepository, never()).save(any());
        verify(prenotazioneRepository, never()).save(any());
    }

    @Test
    void effettuaPrenotazioneParcheggioNonTrovatoTest() {
        LocalDateTime dataCreazione = LocalDateTime.of(2025, 1, 1, 12, 0);

        when(parcheggioRepository.findById("missing")).thenReturn(Optional.empty());

        PrenotazioneRequest req = new PrenotazioneRequest("u1", "missing", dataCreazione);

        assertThrows(IllegalArgumentException.class, () -> service.effettuaPrenotazione(req));
        verify(parcheggioRepository).findById("missing");
        verify(parcheggioRepository, never()).save(any());
        verify(prenotazioneRepository, never()).save(any());
    }

    @Test
    void cercaViciniTest() {
        Parcheggio vicino = new Parcheggio("Vicino", "Centro", 50, 10, 45.50, 9.20);
        Parcheggio lontano = new Parcheggio("Lontano", "Centro", 50, 10, 46.00, 10.00);
        when(parcheggioRepository.findAll()).thenReturn(List.of(vicino, lontano));

        List<ParcheggioResponse> result = service.cercaVicini(45.50, 9.20, 500.0);

        assertEquals(1, result.size());
        assertEquals("Vicino", result.get(0).nome());
        verify(parcheggioRepository).findAll();
    }
}
