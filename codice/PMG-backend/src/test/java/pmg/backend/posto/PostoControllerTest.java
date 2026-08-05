package pmg.backend.posto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test MVC del controller dedicato ai posti embedded nei parcheggi.
 */
@WebMvcTest(PostoController.class)
class PostoControllerTest {

    /** Mock MVC utilizzato per invocare gli endpoint REST. */
    @Autowired
    private MockMvc mockMvc;

    /** Servizio dei posti simulato. */
    @MockBean
    private PostoService postoService;

    /** Verifica il recupero di tutti i posti senza filtro sul piano. */
    @Test
    void getByParcheggio_senzaPiano() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.LIBERO, false);

        when(postoService.getPostiByParcheggio(eq("p1"), isNull()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/posti/parcheggio/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId").value("1-01"))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[0].disponibile").value(true))
                .andExpect(jsonPath("$[0].distanzaUscita").value(4));

        verify(postoService).getPostiByParcheggio("p1", null);
    }

    /** Verifica il recupero dei posti filtrati per piano. */
    @Test
    void getByParcheggio_conPiano() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.LIBERO, false);

        when(postoService.getPostiByParcheggio("p1", 1))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/posti/parcheggio/p1")
                        .param("piano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].piano").value(1));

        verify(postoService).getPostiByParcheggio("p1", 1);
    }

    /** Verifica l'endpoint POST di generazione dei posti. */
    @Test
    void generaPosti_ok() throws Exception {
        doNothing().when(postoService).generaPosti("p1");

        mockMvc.perform(post("/api/posti/genera/p1"))
                .andExpect(status().isNoContent());

        verify(postoService).generaPosti("p1");
    }

    /** Verifica il mantenimento dell'endpoint legacy GET. */
    @Test
    void generaPostiLegacy_ok() throws Exception {
        doNothing().when(postoService).generaPosti("p1");

        mockMvc.perform(get("/api/posti/genera/p1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Posti generati o sincronizzati"));

        verify(postoService).generaPosti("p1");
    }

    /** Verifica l'aggiornamento legacy della disponibilità. */
    @Test
    void updateDisponibilita_ok() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.PRENOTATO, false);

        when(postoService.aggiornaDisponibilita(
                "p1", 1, 1, false))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/posti/parcheggio/p1/piano/1/numero/1/disponibilita")
                        .param("disponibile", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibile").value(false))
                .andExpect(jsonPath("$.stato").value("PRENOTATO"));

        verify(postoService).aggiornaDisponibilita(
                "p1", 1, 1, false);
    }

    /** Verifica l'aggiornamento legacy della disabilitazione. */
    @Test
    void updateDisabilitato_ok() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.LIBERO, true);

        when(postoService.aggiornaDisabilitato(
                "p1", 1, 1, true))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/posti/parcheggio/p1/piano/1/numero/1/disabilitato")
                        .param("disabilitato", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabilitato").value(true))
                .andExpect(jsonPath("$.fuoriServizio").value(true))
                .andExpect(jsonPath("$.disponibile").value(false));

        verify(postoService).aggiornaDisabilitato(
                "p1", 1, 1, true);
    }

    /** Verifica l'aggiornamento della messa fuori servizio tramite slotId. */
    @Test
    void updateFuoriServizio_ok() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.LIBERO, true);

        when(postoService.aggiornaFuoriServizio(
                "p1", "1-01", true))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/posti/parcheggio/p1/slot/1-01/fuori-servizio")
                        .param("fuoriServizio", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value("1-01"))
                .andExpect(jsonPath("$.fuoriServizio").value(true));

        verify(postoService).aggiornaFuoriServizio(
                "p1", "1-01", true);
    }

    /** Verifica l'aggiornamento dello stato operativo tramite slotId. */
    @Test
    void updateStato_ok() throws Exception {
        PostoResponse response = creaPostoResponse(
                StatoPosto.OCCUPATO, false);

        when(postoService.aggiornaStato(
                "p1", "1-01", StatoPosto.OCCUPATO))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/posti/parcheggio/p1/slot/1-01/stato")
                        .param("stato", "OCCUPATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("OCCUPATO"))
                .andExpect(jsonPath("$.disponibile").value(false));

        verify(postoService).aggiornaStato(
                "p1", "1-01", StatoPosto.OCCUPATO);
    }

    /** Crea una risposta posto coerente con il nuovo dominio embedded. */
    private PostoResponse creaPostoResponse(
            StatoPosto stato,
            boolean fuoriServizio) {
        Posto posto = new Posto(
                "1-01",
                1,
                "P1-01",
                1,
                TipoPosto.NORMALE,
                stato,
                fuoriServizio,
                4);
        return new PostoResponse(posto, "p1");
    }
}
