package pmg.backend.utente;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pmg.backend.utente.UtenteController;
import pmg.backend.utente.UtenteLoginRequest;
import pmg.backend.utente.UtenteRegisterRequest;
import pmg.backend.utente.UtenteResponse;
import pmg.backend.utente.UtenteService;

@WebMvcTest(UtenteController.class) // Carica solo lo strato Web per UtenteController
public class UtenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtenteService utenteService; // Mockiamo il servizio per isolare il controller

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegistrazioneSuccess() throws Exception {
        // Dati di esempio (Usiamo record se definiti come tali nel tuo progetto)
        UtenteRegisterRequest req = new UtenteRegisterRequest("Mario", "Rossi", "mario@test.it", "secret");
        UtenteResponse resp = new UtenteResponse("u1", "Mario", "Rossi", "mario@test.it", "mario.rossi", new HashMap<>());

        when(utenteService.registrazione(any(UtenteRegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/utenti/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.username").value("mario.rossi"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        UtenteLoginRequest req = new UtenteLoginRequest("mario@test.it", "secret");
        UtenteResponse resp = new UtenteResponse("u1", "Mario", "Rossi", "mario@test.it", "mario.rossi", new HashMap<>());

        when(utenteService.login(any(UtenteLoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/utenti/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mario@test.it"));
    }

    @Test
    void testGetPreferenze() throws Exception {
        Map<String, String> pref = new HashMap<>();
        pref.put("lingua", "it");
        
        when(utenteService.getPreferenze("u1")).thenReturn(pref);

        mockMvc.perform(get("/api/utenti/u1/preferenze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lingua").value("it"));
    }

    @Test
    void testAggiornaPreferenze() throws Exception {
        Map<String, String> pref = new HashMap<>();
        pref.put("notifiche", "true");

        mockMvc.perform(put("/api/utenti/u1/preferenze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pref)))
                .andExpect(status().isNoContent()); // Verifica il 204 No Content
    }

    @Test
    void testDeleteUtente() throws Exception {
        mockMvc.perform(delete("/api/utenti/u1"))
                .andExpect(status().isNoContent());
    }
}