package pmg.backend.prenotazione;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrenotazioneController.class)
class PrenotazioneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrenotazioneService prenotazioneService;

    @Test
    void getStoricoTest() throws Exception {
        LocalDateTime orario = LocalDateTime.of(2025, 1, 1, 10, 0);

        PrenotazioneResponse r1 = new PrenotazioneResponse("p1", "u1", "park1", orario, "QR1");
        PrenotazioneResponse r2 = new PrenotazioneResponse("p2", "u1", "park2", orario.plusHours(1), "QR2");

        when(prenotazioneService.getStoricoUtente("u1")).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/prenotazioni/utente/u1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].utenteId").value("u1"))
                .andExpect(jsonPath("$[0].parcheggioId").value("park1"))
                .andExpect(jsonPath("$[0].codiceQr").value("QR1"))
                .andExpect(jsonPath("$[1].id").value("p2"))
                .andExpect(jsonPath("$[1].codiceQr").value("QR2"));

        verify(prenotazioneService).getStoricoUtente("u1");
    }
}
