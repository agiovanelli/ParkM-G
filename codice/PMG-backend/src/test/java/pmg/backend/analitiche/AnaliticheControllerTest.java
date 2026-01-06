package pmg.backend.analitiche;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import pmg.backend.log.Log;
import pmg.backend.log.LogCategoria;
import pmg.backend.log.LogSeverità;
import pmg.backend.log.LogService;

@WebMvcTest(AnaliticheController.class)
class AnaliticheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnaliticheService analiticheService;

    @MockBean
    private LogService logService;

    @Test
    void getByIdTest() throws Exception {
        Analitiche a = new Analitiche("P1", "Parcheggio A", "op-1");
        ReflectionTestUtils.setField(a, "id", "123");

        when(analiticheService.getById("123")).thenReturn(a);

        mockMvc.perform(get("/api/analitiche/123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.parcheggioId").value("P1"))
                .andExpect(jsonPath("$.nomeParcheggio").value("Parcheggio A"))
                .andExpect(jsonPath("$.operatoreId").value("op-1"));
    }

    @Test
    void getByOperatoreIdTest() throws Exception {
        Analitiche a = new Analitiche("P2", "Parcheggio B", "op-2");
        ReflectionTestUtils.setField(a, "id", "A-1");

        when(analiticheService.getByOperatoreId("op-2")).thenReturn(a);

        mockMvc.perform(get("/api/analitiche/operatore/op-2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("A-1"))
                .andExpect(jsonPath("$.parcheggioId").value("P2"))
                .andExpect(jsonPath("$.nomeParcheggio").value("Parcheggio B"))
                .andExpect(jsonPath("$.operatoreId").value("op-2"));
    }

    @Test
    void creaAnaliticheTest() throws Exception {
        Analitiche saved = new Analitiche("P3", "Parcheggio C", "op-3");
        ReflectionTestUtils.setField(saved, "id", "999");

        when(analiticheService.save(any(AnaliticheRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/analitiche")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parcheggioId": "P3",
                      "nomeParcheggio": "Parcheggio C",
                      "operatoreId": "op-3"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("999"))
                .andExpect(jsonPath("$.parcheggioId").value("P3"))
                .andExpect(jsonPath("$.nomeParcheggio").value("Parcheggio C"))
                .andExpect(jsonPath("$.operatoreId").value("op-3"))
                .andExpect(jsonPath("$.log").isArray())
                .andExpect(jsonPath("$.log").isEmpty());
    }

    @Test
    void getLogByAnaliticaIdTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log1");
        ReflectionTestUtils.setField(l, "analiticaId", "123");
        l.setTipo(LogCategoria.EVENTO);
        l.setSeverita(LogSeverità.INFO);
        l.setTitolo("Titolo1");
        l.setDescrizione("Descrizione1");
        l.setData(LocalDateTime.of(2025, 1, 1, 12, 0));

        when(logService.getLogByAnaliticaId("123")).thenReturn(List.of(l));

        mockMvc.perform(get("/api/analitiche/123/log"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("log1"))
                .andExpect(jsonPath("$[0].tipo").value("EVENTO"))
                .andExpect(jsonPath("$[0].titolo").value("Titolo1"))
                .andExpect(jsonPath("$[0].descrizione").value("Descrizione1"))
                .andExpect(jsonPath("$[0].severita").value("INFO"))
                .andExpect(jsonPath("$[0].data").value("2025-01-01T12:00:00"));
    }

    @Test
    void getLogByAnaliticaIdAndTipoTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log2");
        ReflectionTestUtils.setField(l, "analiticaId", "123");
        l.setTipo(LogCategoria.ALLARME);
        l.setSeverita(LogSeverità.CRITICO);
        l.setTitolo("Allarme");
        l.setDescrizione("Dettagli");
        l.setData(LocalDateTime.of(2025, 2, 2, 10, 30));

        when(logService.getLogByAnaliticaIdAndTipo("123", "ALLARME")).thenReturn(List.of(l));

        mockMvc.perform(get("/api/analitiche/123/log/ALLARME"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("log2"))
                .andExpect(jsonPath("$[0].tipo").value("ALLARME"))
                .andExpect(jsonPath("$[0].severita").value("CRITICO"))
                .andExpect(jsonPath("$[0].data").value("2025-02-02T10:30:00"));

        verify(logService).getLogByAnaliticaIdAndTipo("123", "ALLARME");
    }
}
