package pmg.backend.prenotazione;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceImplTest {

    @Mock
    private PrenotazioneRepository prenotazioneRepository;

    @InjectMocks
    private PrenotazioneServiceImpl service;

    @Test
    void getStoricoUtenteTest() {
        LocalDateTime orario1 = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime orario2 = LocalDateTime.of(2025, 1, 1, 11, 0);

        Prenotazione p1 = new Prenotazione("u1", "park1", orario1, "QR1");
        p1.setId("p1");

        Prenotazione p2 = new Prenotazione("u1", "park2", orario2, "QR2");
        p2.setId("p2");

        when(prenotazioneRepository.findByUtenteId("u1")).thenReturn(List.of(p1, p2));

        List<PrenotazioneResponse> result = service.getStoricoUtente("u1");

        assertEquals(2, result.size());

        assertEquals("p1", result.get(0).id());
        assertEquals("u1", result.get(0).utenteId());
        assertEquals("park1", result.get(0).parcheggioId());
        assertEquals(orario1, result.get(0).orario());
        assertEquals("QR1", result.get(0).codiceQr());

        assertEquals("p2", result.get(1).id());
        assertEquals("u1", result.get(1).utenteId());
        assertEquals("park2", result.get(1).parcheggioId());
        assertEquals(orario2, result.get(1).orario());
        assertEquals("QR2", result.get(1).codiceQr());

        verify(prenotazioneRepository).findByUtenteId("u1");
        verifyNoMoreInteractions(prenotazioneRepository);
    }
}
