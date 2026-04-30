package pmg.backend.parcheggio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].nome").value("Parcheggio A"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].nome").value("Parcheggio B"));

        verify(parcheggioService).cercaPerArea("Centro");
    }

    @Test
    void prenotaTest() throws Exception {
        LocalDateTime dataCreazione = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime scadenza = dataCreazione.plusMinutes(10);

        PrenotazioneResponse resp = new PrenotazioneResponse(
                "1",
                "utente1",
                "parcheggio1",
                dataCreazione,
                "QR123",
                null,
                null,
                null,
                null,
                null,
                scadenza
        );

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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.utenteId").value("utente1"))
                .andExpect(jsonPath("$.parcheggioId").value("parcheggio1"))
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("3"))
                .andExpect(jsonPath("$[0].nome").value("Vicino"))
                .andExpect(jsonPath("$[0].area").value("Nord"));

        verify(parcheggioService).cercaVicini(45.50, 9.20, 500.0);
    }

    @Test
    void toggleEmergenzaTest() throws Exception {
        mockMvc.perform(patch("/api/parcheggi/p1/emergenza")
                .param("attiva", "true")
                .param("motivo", "Incendio"))
                .andExpect(status().isOk());

        verify(parcheggioService).impostaStatoEmergenza("p1", true, "Incendio");
    }

    @Test
    void getByIdTest() throws Exception {
        ParcheggioResponse response = new ParcheggioResponse(
                "p1", "Parcheggio Centro", "Centro", 100, 45, 45.55, 9.22, false
        );

        when(parcheggioService.getById("p1")).thenReturn(response);

        mockMvc.perform(get("/api/parcheggi/p1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.nome").value("Parcheggio Centro"))
                .andExpect(jsonPath("$.area").value("Centro"))
                .andExpect(jsonPath("$.postiTotali").value(100))
                .andExpect(jsonPath("$.postiDisponibili").value(45));

        verify(parcheggioService).getById("p1");
    }

    @Test
    void getPostiTest() throws Exception {
        Posto posto = Mockito.mock(Posto.class);
        when(posto.getId()).thenReturn("posto-1");
        when(posto.getNumero()).thenReturn(12);
        when(posto.getPiano()).thenReturn(1);
        when(posto.isDisponibile()).thenReturn(true);
        when(posto.isDisabilitato()).thenReturn(false);
        when(posto.isRiservatoDisabili()).thenReturn(false);
        when(posto.isRiservatoIncinta()).thenReturn(false);
        when(posto.getParcheggioId()).thenReturn("p1");
        when(posto.getDistanzaUscita()).thenReturn(1);

        PostoResponse response = new PostoResponse(posto);

        when(parcheggioService.getPosti("p1", 1)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/parcheggi/p1/posti")
                .param("piano", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(parcheggioService).getPosti("p1", 1);
    }

    @Test
    void syncPostiTest() throws Exception {
        mockMvc.perform(put("/api/parcheggi/p1/sync"))
                .andExpect(status().isOk());

        verify(parcheggioService).syncPostiStats("p1");
    }
}