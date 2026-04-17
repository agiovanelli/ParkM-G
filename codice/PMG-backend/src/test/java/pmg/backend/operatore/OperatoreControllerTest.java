package pmg.backend.operatore;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperatoreController.class)
class OperatoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperatoreService operatoreService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginSuccess() throws Exception {
        OperatoreLoginRequest request = new OperatoreLoginRequest("strutturatest", "admin");

        OperatoreResponse fakeResponse = new OperatoreResponse(
                "123",
                "admin",
                "strutturatest",
                "park1"
        );

        when(operatoreService.login(any(OperatoreLoginRequest.class))).thenReturn(fakeResponse);

        mockMvc.perform(post("/api/operatori/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nomeStruttura").value("strutturatest"))
                .andExpect(jsonPath("$.parcheggioId").value("park1"));

        verify(operatoreService).login(any(OperatoreLoginRequest.class));
    }
}