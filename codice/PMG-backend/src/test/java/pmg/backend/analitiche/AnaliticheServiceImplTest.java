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
        // given
        Analitiche a = new Analitiche("P1", "Parcheggio A", "op-1");
        when(repository.findById("123")).thenReturn(Optional.of(a));

        // when
        Analitiche result = service.getById("123");

        // then
        assertSame(a, result);
        verify(repository).findById("123");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdEccezioneTest() {
        // given
        when(repository.findById("404")).thenReturn(Optional.empty());

        // when + then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getById("404"));
        assertEquals("Analitiche non trovata", ex.getMessage());

        verify(repository).findById("404");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByOperatoreIdTest() {
        // given
        Analitiche a = new Analitiche("P2", "Parcheggio B", "op-2");
        when(repository.findByOperatoreId("op-2")).thenReturn(Optional.of(a));

        // when
        Analitiche result = service.getByOperatoreId("op-2");

        // then
        assertSame(a, result);
        verify(repository).findByOperatoreId("op-2");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByOperatoreIdEccezioneTest() {
        // given
        when(repository.findByOperatoreId("op-x")).thenReturn(Optional.empty());

        // when + then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getByOperatoreId("op-x"));
        assertEquals("Analitiche non trovata", ex.getMessage());

        verify(repository).findByOperatoreId("op-x");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void saveTest() {
        // given
        AnaliticheRequest req = new AnaliticheRequest("P3", "Parcheggio C", "op-3");

        // catturo l'entity passata a repository.save(...)
        ArgumentCaptor<Analitiche> captor = ArgumentCaptor.forClass(Analitiche.class);

        // faccio ritornare al repository lo stesso oggetto ricevuto
        when(repository.save(any(Analitiche.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Analitiche saved = service.save(req);

        // then: verifica che abbia chiamato save e che il mapping sia corretto
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
}
