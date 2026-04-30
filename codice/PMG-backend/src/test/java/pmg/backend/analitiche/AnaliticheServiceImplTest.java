package pmg.backend.analitiche;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnaliticheServiceImplTest {

    @Mock
    private AnaliticheRepository repository;

    @InjectMocks
    private AnaliticheServiceImpl service;

    @Test
    void getByIdTest() {
        Analitiche a = new Analitiche("P1", "Parcheggio A", "op-1");
        when(repository.findById("123")).thenReturn(Optional.of(a));

        Analitiche result = service.getById("123");

        assertSame(a, result);
        verify(repository).findById("123");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdEccezioneTest() {
        when(repository.findById("404")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getById("404"));
        assertEquals("Analitiche non trovata", ex.getMessage());

        verify(repository).findById("404");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByOperatoreIdTest() {
        Analitiche a = new Analitiche("P2", "Parcheggio B", "op-2");
        when(repository.findByOperatoreId("op-2")).thenReturn(Optional.of(a));

        Analitiche result = service.getByOperatoreId("op-2");

        assertSame(a, result);
        verify(repository).findByOperatoreId("op-2");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByOperatoreIdEccezioneTest() {
        when(repository.findByOperatoreId("op-x")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getByOperatoreId("op-x"));
        assertEquals("Analitiche non trovata", ex.getMessage());

        verify(repository).findByOperatoreId("op-x");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void saveTest() {
        AnaliticheRequest req = new AnaliticheRequest("P3", "Parcheggio C", "op-3");

        ArgumentCaptor<Analitiche> captor = ArgumentCaptor.forClass(Analitiche.class);

        when(repository.save(any(Analitiche.class))).thenAnswer(inv -> inv.getArgument(0));

        Analitiche saved = service.save(req);

        verify(repository).save(captor.capture());
        Analitiche entityPassed = captor.getValue();

        assertEquals("P3", entityPassed.getParcheggioId());
        assertEquals("Parcheggio C", entityPassed.getNomeParcheggio());
        assertEquals("op-3", entityPassed.getOperatoreId());

        assertEquals("P3", saved.getParcheggioId());
        assertEquals("Parcheggio C", saved.getNomeParcheggio());
        assertEquals("op-3", saved.getOperatoreId());

        verifyNoMoreInteractions(repository);
    }
    
    @Test
    void getByParcheggioIdTest() {
        Analitiche a = new Analitiche("P5", "Parcheggio E", "op-5");
        when(repository.findByParcheggioId("P5")).thenReturn(Optional.of(a));

        Analitiche result = service.getByParcheggioId("P5");

        assertSame(a, result);
        verify(repository).findByParcheggioId("P5");
        verifyNoMoreInteractions(repository);
    }
    
    @Test
    void getByParcheggioIdEccezioneTest() {
        when(repository.findByParcheggioId("PX")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getByParcheggioId("PX"));

        assertEquals("Analitiche non trovata per parcheggioId: PX", ex.getMessage());

        verify(repository).findByParcheggioId("PX");
        verifyNoMoreInteractions(repository);
    }
}
