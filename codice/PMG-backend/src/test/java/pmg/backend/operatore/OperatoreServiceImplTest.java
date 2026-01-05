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
    private OperatoreRepository repository; // "Fingiamo" il database MongoDB

    @InjectMocks
    private OperatoreServiceImpl service; // La classe che stiamo testando

    private OperatoreLoginRequest validRequest;
    private Operatore mockOperatore;

    @BeforeEach
    void setUp() {
        // Prepariamo i dati comuni a tutti i test
        validRequest = new OperatoreLoginRequest("strutturatest", "admin");
        
        mockOperatore = new Operatore();
        mockOperatore.setId("123");
        mockOperatore.setUsername("admin");
        mockOperatore.setNomeStruttura("strutturatest");
    }

    @Test
    void testLoginSuccess() {
        // GIVEN: Quando cerchi l'operatore, il database deve rispondere che esiste
        when(repository.findByNomeStrutturaAndUsername("strutturatest", "admin"))
                .thenReturn(Optional.of(mockOperatore));

        // WHEN: Eseguiamo il login
        OperatoreResponse result = service.login(validRequest);

        // THEN: Verifichiamo che i dati restituiti siano corretti
        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("admin", result.getUsername());
        assertEquals("strutturatest", result.getNomeStruttura());
        verify(repository, times(1)).findByNomeStrutturaAndUsername(anyString(), anyString());
    }

    @Test
    void testLoginFailure_NotFound() {
        // GIVEN: Quando cerchi l'operatore, il database risponde vuoto
        when(repository.findByNomeStrutturaAndUsername(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // WHEN & THEN: Verifichiamo che venga lanciata l'eccezione corretta
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.login(validRequest);
        });

        assertEquals("Operatore non registrato", exception.getMessage());
    }
}