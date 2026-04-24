package pmg.backend.posto;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostoController.class)
class PostoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostoService postoService;

    private PostoResponse creaPostoResponse() {
    	Posto posto = new Posto("1", "p1", 1, 0, new PostoBoolean(true, false, false, false), 4);
        return new PostoResponse(posto);
    }

    @Test
    void getByParcheggio_senzaPiano() throws Exception {
        PostoResponse response = creaPostoResponse();

        Mockito.when(postoService.getPostiByParcheggio(eq("p1"), isNull()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/posti/parcheggio/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[0].disponibile").value(true))
                .andExpect(jsonPath("$[0].distanzaUscita").value(4));
    }

    @Test
    void getByParcheggio_conPiano() throws Exception {
        PostoResponse response = creaPostoResponse();

        Mockito.when(postoService.getPostiByParcheggio("p1", 0))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/posti/parcheggio/p1")
                        .param("piano", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].piano").value(0));
    }

    @Test
    void generaPosti_ok() throws Exception {
        Mockito.doNothing().when(postoService).generaPosti("p1");

        mockMvc.perform(get("/api/posti/genera/p1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Posti generati!"));
    }

    @Test
    void updateDisponibilita_ok() throws Exception {
    	Posto posto = new Posto("1", "p1", 1, 0, new PostoBoolean(false, false, false, false), 4);

        PostoResponse response = new PostoResponse(posto);

        Mockito.when(postoService.aggiornaDisponibilita("p1", 0, 1, false))
                .thenReturn(response);

        mockMvc.perform(patch("/api/posti/parcheggio/p1/piano/0/numero/1/disponibilita")
                        .param("disponibile", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibile").value(false));
    }

    @Test
    void updateDisabilitato_ok() throws Exception {
        Posto posto = new Posto("1", "p1", 1, 0, new PostoBoolean(true, true, false, false), 4);

        PostoResponse response = new PostoResponse(posto);

        Mockito.when(postoService.aggiornaDisabilitato("p1", 0, 1, true))
                .thenReturn(response);

        mockMvc.perform(patch("/api/posti/parcheggio/p1/piano/0/numero/1/disabilitato")
                        .param("disabilitato", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabilitato").value(true));
    }
}