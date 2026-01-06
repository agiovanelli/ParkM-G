package pmg.backend.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock
    private LogRepository repository;

    @InjectMocks
    private LogServiceImpl service;

    @Test
    void getLogByIdTest() {
        Log log = new Log();
        ReflectionTestUtils.setField(log, "id", "log1");
        when(repository.findById("log1")).thenReturn(Optional.of(log));

        Optional<Log> result = service.getLogById("log1");

        assertTrue(result.isPresent());
        assertEquals("log1", result.get().getId());
        verify(repository).findById("log1");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getLogByIdVuotoTest() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        Optional<Log> result = service.getLogById("missing");

        assertTrue(result.isEmpty());
        verify(repository).findById("missing");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void salvaLog1Test() {
        Log log = new Log();
        log.setTitolo("Titolo");
        log.setDescrizione("Descrizione");
        log.setSeverita(LogSeverità.INFO);
        log.setTipo(LogCategoria.EVENTO);

        when(repository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        Log saved = service.salvaLog1(log);

        // data impostata "ora"
        assertNotNull(saved.getData());
        assertEquals("Titolo", saved.getTitolo());
        assertEquals("Descrizione", saved.getDescrizione());
        assertEquals(LogSeverità.INFO, saved.getSeverita());
        assertEquals(LogCategoria.EVENTO, saved.getTipo());

        verify(repository).save(any(Log.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void salvaLogTest() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 12, 0);
        LogRequest req = new LogRequest("a1", LogCategoria.ALLARME, LogSeverità.CRITICO, "T", "D", time);

        ArgumentCaptor<Log> captor = ArgumentCaptor.forClass(Log.class);

        when(repository.save(any(Log.class))).thenAnswer(inv -> inv.getArgument(0));

        Log saved = service.salvaLog(req);

        verify(repository).save(captor.capture());
        Log passed = captor.getValue();

        assertEquals(LogCategoria.ALLARME, passed.getTipo());
        assertEquals(LogSeverità.CRITICO, passed.getSeverita());
        assertEquals("T", passed.getTitolo());
        assertEquals("D", passed.getDescrizione());
        assertEquals(time, passed.getData());

        assertEquals(passed.getTitolo(), saved.getTitolo());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getLogByAnaliticaIdTest() {
        Log l1 = new Log();
        ReflectionTestUtils.setField(l1, "id", "L1");
        Log l2 = new Log();
        ReflectionTestUtils.setField(l2, "id", "L2");

        when(repository.findByAnaliticaIdOrderByDataDesc("A1")).thenReturn(List.of(l1, l2));

        List<Log> result = service.getLogByAnaliticaId("A1");

        assertEquals(2, result.size());
        assertEquals("L1", result.get(0).getId());
        verify(repository).findByAnaliticaIdOrderByDataDesc("A1");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getLogByAnaliticaIdAndTipoTest() {
        Log l = new Log();
        ReflectionTestUtils.setField(l, "id", "L3");

        when(repository.findByAnaliticaIdAndTipo("A2", "EVENTO")).thenReturn(List.of(l));

        List<Log> result = service.getLogByAnaliticaIdAndTipo("A2", "EVENTO");

        assertEquals(1, result.size());
        assertEquals("L3", result.get(0).getId());
        verify(repository).findByAnaliticaIdAndTipo("A2", "EVENTO");
        verifyNoMoreInteractions(repository);
    }
}
