package pmg.backend.log;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogService service;

    // ---------- POST /api/log ----------
    @Test
    void creaLogTest() throws Exception {
        Log saved = new Log();
        ReflectionTestUtils.setField(saved, "id", "log1");
        ReflectionTestUtils.setField(saved, "analiticaId", "a1");
        saved.setTipo(LogCategoria.EVENTO);
        saved.setSeverita(LogSeverità.INFO);
        saved.setTitolo("Titolo");
        saved.setDescrizione("Descrizione");
        saved.setData(LocalDateTime.of(2025, 1, 1, 12, 0));

        when(service.salvaLog(any(LogRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "analiticaId": "a1",
                      "tipo": "EVENTO",
                      "severita": "INFO",
                      "titolo": "Titolo",
                      "descrizione": "Descrizione",
                      "data": "2025-01-01T12:00:00"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("log1"))
                .andExpect(jsonPath("$.tipo").value("EVENTO"))
                .andExpect(jsonPath("$.titolo").value("Titolo"))
                .andExpect(jsonPath("$.descrizione").value("Descrizione"))
                .andExpect(jsonPath("$.data").value("2025-01-01T12:00:00"))
                .andExpect(jsonPath("$.severita").value("INFO"));

        verify(service).salvaLog(any(LogRequest.class));
    }

    // ---------- GET /api/log/analitiche/{analiticaId}/log ----------
    @Test
    void getLogByAnaliticaIdTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log1");
        ReflectionTestUtils.setField(l, "analiticaId", "a1");
        l.setTipo(LogCategoria.ALLARME);
        l.setSeverita(LogSeverità.CRITICO);
        l.setTitolo("Allarme");
        l.setDescrizione("Dettagli");
        l.setData(LocalDateTime.of(2025, 2, 2, 10, 30));

        when(service.getLogByAnaliticaId("a1")).thenReturn(List.of(l));

        mockMvc.perform(get("/api/log/analitiche/a1/log"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("log1"))
                .andExpect(jsonPath("$[0].tipo").value("ALLARME"))
                .andExpect(jsonPath("$[0].titolo").value("Allarme"))
                .andExpect(jsonPath("$[0].descrizione").value("Dettagli"))
                .andExpect(jsonPath("$[0].data").value("2025-02-02T10:30:00"))
                .andExpect(jsonPath("$[0].severita").value("CRITICO"));

        verify(service).getLogByAnaliticaId("a1");
    }

    // ---------- GET /api/log/analitiche/{analiticaId}/tipo/{tipo} ----------
    @Test
    void getLogByAnaliticaIdAndTipoTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log2");
        ReflectionTestUtils.setField(l, "analiticaId", "a1");
        l.setTipo(LogCategoria.EVENTO);
        l.setSeverita(LogSeverità.INFO);
        l.setTitolo("Evento");
        l.setDescrizione("Desc");
        l.setData(LocalDateTime.of(2025, 3, 3, 9, 0));

        when(service.getLogByAnaliticaIdAndTipo("a1", "EVENTO")).thenReturn(List.of(l));

        mockMvc.perform(get("/api/log/analitiche/a1/tipo/EVENTO"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("log2"))
                .andExpect(jsonPath("$[0].tipo").value("EVENTO"))
                .andExpect(jsonPath("$[0].severita").value("INFO"));

        verify(service).getLogByAnaliticaIdAndTipo("a1", "EVENTO");
    }

    // ---------- PUT /api/log/{id}/severity?severity=... ----------
    @Test
    void aggiornaSeverityTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log9");
        l.setTipo(LogCategoria.HISTORY);
        l.setSeverita(LogSeverità.ATTENZIONE);
        l.setTitolo("T");
        l.setDescrizione("D");
        l.setData(LocalDateTime.of(2025, 4, 4, 8, 0));

        Log savedAfter = new Log();
        ReflectionTestUtils.setField(savedAfter, "id", "log9");
        savedAfter.setTipo(LogCategoria.HISTORY);
        savedAfter.setSeverita(LogSeverità.CRITICO); // aggiornato
        savedAfter.setTitolo("T");
        savedAfter.setDescrizione("D");
        savedAfter.setData(LocalDateTime.of(2025, 4, 4, 8, 0));

        when(service.getLogById("log9")).thenReturn(Optional.of(l));
        when(service.salvaLog1(any(Log.class))).thenReturn(savedAfter);

        mockMvc.perform(put("/api/log/log9/severity")
                .param("severity", "CRITICO"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("log9"))
                .andExpect(jsonPath("$.severita").value("CRITICO"));

        verify(service).getLogById("log9");
        verify(service).salvaLog1(any(Log.class));
    }

    // ---------- PUT /api/log/{id}/category?category=... ----------
    @Test
    void aggiornaCategoryTest() throws Exception {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "log7");
        l.setTipo(LogCategoria.EVENTO);
        l.setSeverita(LogSeverità.INFO);
        l.setTitolo("T");
        l.setDescrizione("D");
        l.setData(LocalDateTime.of(2025, 5, 5, 7, 0));

        Log savedAfter = new Log();
        ReflectionTestUtils.setField(savedAfter, "id", "log7");
        savedAfter.setTipo(LogCategoria.ALLARME);
        savedAfter.setSeverita(LogSeverità.INFO);
        savedAfter.setTitolo("T");
        savedAfter.setDescrizione("D");
        savedAfter.setData(LocalDateTime.of(2025, 5, 5, 7, 0));

        when(service.getLogById("log7")).thenReturn(Optional.of(l));
        when(service.salvaLog1(any(Log.class))).thenReturn(savedAfter);

        mockMvc.perform(put("/api/log/log7/category")
                .param("category", "ALLARME"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("log7"))
                .andExpect(jsonPath("$.tipo").value("ALLARME"));

        verify(service).getLogById("log7");
        verify(service).salvaLog1(any(Log.class));
    }
}
