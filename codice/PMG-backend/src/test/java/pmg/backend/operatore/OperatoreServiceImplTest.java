package pmg.backend.operatore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatoreServiceImplTest {

    @Mock
    private OperatoreRepository repository;

    @InjectMocks
    private OperatoreServiceImpl service;

    private OperatoreLoginRequest validRequest;
    private Operatore mockOperatore;

    @BeforeEach
    void setUp() {
        validRequest = new OperatoreLoginRequest("strutturatest", "admin");

        mockOperatore = new Operatore();
        mockOperatore.setId("123");
        mockOperatore.setUsername("admin");
        mockOperatore.setNomeStruttura("strutturatest");
        mockOperatore.setParcheggioId("park1");
    }

    @Test
    void testLoginSuccess() {
        when(repository.findByNomeStrutturaAndUsername("strutturatest", "admin"))
                .thenReturn(Optional.of(mockOperatore));

        OperatoreResponse result = service.login(validRequest);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("admin", result.getUsername());
        assertEquals("strutturatest", result.getNomeStruttura());
        assertEquals("park1", result.getParcheggioId());

        verify(repository).findByNomeStrutturaAndUsername("strutturatest", "admin");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void testLoginFailure_NotFound() {
        when(repository.findByNomeStrutturaAndUsername("strutturatest", "admin"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.login(validRequest)
        );

        assertEquals("Operatore non registrato", exception.getMessage());

        verify(repository).findByNomeStrutturaAndUsername("strutturatest", "admin");
        verifyNoMoreInteractions(repository);
    }
}