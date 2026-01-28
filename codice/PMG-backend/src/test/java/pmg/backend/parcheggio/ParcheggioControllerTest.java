package pmg.backend.parcheggio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

@WebMvcTest(ParcheggioController.class)
class ParcheggioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParcheggioService parcheggioService;

    @Test
    void cercaTest() throws Exception {
        ParcheggioResponse p1 = new ParcheggioResponse("1", "Parcheggio A", "Centro", 100, 20, 45.5, 9.1, false);
        ParcheggioResponse p2 = new ParcheggioResponse("2", "Parcheggio B", "Centro", 50, 10, 45.51, 9.11, false);

        when(parcheggioService.cercaPerArea("Centro")).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/parcheggi/cerca").param("area", "Centro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].nome").value("Parcheggio B"));

        verify(parcheggioService).cercaPerArea("Centro");
    }

    @Test
    void prenotaTest() throws Exception {
        LocalDateTime dataCreazione = LocalDateTime.of(2025, 1, 1, 10, 0);

        PrenotazioneResponse resp = new PrenotazioneResponse("1", "utente1", "parcheggio1", dataCreazione, "QR123", null, dataCreazione, dataCreazione);

        when(parcheggioService.effettuaPrenotazione(any(PrenotazioneRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/parcheggi/prenota")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "utenteId": "utente1",
                      "parcheggioId": "parcheggio1",
                      "dataCreazione": "2025-01-01T10:00:00"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.codiceQr").value("QR123"))
                .andExpect(jsonPath("$.dataCreazione").value("2025-01-01T10:00:00"));

        verify(parcheggioService).effettuaPrenotazione(any(PrenotazioneRequest.class));
    }

    @Test
    void getNearbyTest() throws Exception {
        ParcheggioResponse vicino = new ParcheggioResponse("3", "Vicino", "Nord", 10, 2, 45.50, 9.20, false);
        when(parcheggioService.cercaVicini(45.50, 9.20, 500.0)).thenReturn(List.of(vicino));

        mockMvc.perform(get("/api/parcheggi/nearby")
                .param("lat", "45.50")
                .param("lng", "9.20")
                .param("radius", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Vicino"))
                .andExpect(jsonPath("$[0].area").value("Nord"));

        verify(parcheggioService).cercaVicini(45.50, 9.20, 500.0);
    }
}
