package pmg.backend.operatore;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(OperatoreController.class) // Carica solo lo stretto necessario per testare questo controller
class OperatoreControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simula il client che fa la chiamata API

    @MockBean
    private OperatoreService operatoreService; // "Fingiamo" il servizio per isolare il controller

    @Autowired
    private ObjectMapper objectMapper; // Serve per trasformare gli oggetti Java in JSON

    @Test
    void testLoginSuccess() throws Exception {
        // GIVEN
        OperatoreLoginRequest request = new OperatoreLoginRequest("strutturatest", "admin");

        OperatoreResponse fakeResponse = new OperatoreResponse();
        // Aggiungi qui eventuali setter per la risposta se necessari

        when(operatoreService.login(any(OperatoreLoginRequest.class))).thenReturn(fakeResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/operatori/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}