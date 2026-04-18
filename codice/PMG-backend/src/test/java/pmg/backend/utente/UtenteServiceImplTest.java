package pmg.backend.utente;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtenteServiceImplTest {

    @Mock
    private UtenteRepository repository;

    @InjectMocks
    private UtenteServiceImpl service;

    private Utente mockUtente;

    @BeforeEach
    void setUp() {
        mockUtente = new Utente("Mario", "Rossi", "mario@test.it", "mario.rossi", "password123");
        mockUtente.setId("user123");
    }

    @Test
    void testRegistrazioneSuccess() {
        UtenteRegisterRequest req = new UtenteRegisterRequest("Mario", "Rossi", "mario@test.it", "password123");
        when(repository.existsByEmail("mario@test.it")).thenReturn(false);
        when(repository.save(any(Utente.class))).thenReturn(mockUtente);

        UtenteResponse resp = service.registrazione(req);

        assertNotNull(resp);
        assertEquals("mario.rossi", resp.getUsername()); // Verifica generazione username
        verify(repository).save(any(Utente.class));
    }

    @Test
    void testRegistrazioneEmailGiaEsistente() {
        UtenteRegisterRequest req = new UtenteRegisterRequest("Mario", "Rossi", "mario@test.it", "password123");
        when(repository.existsByEmail("mario@test.it")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.registrazione(req));
    }

    @Test
    void testLoginSuccess() {
        UtenteLoginRequest req = new UtenteLoginRequest("mario@test.it", "password123");
        when(repository.findByEmailAndPassword("mario@test.it", "password123"))
                .thenReturn(Optional.of(mockUtente));

        UtenteResponse resp = service.login(req);

        assertEquals("user123", resp.getId());
        assertEquals("mario@test.it", resp.getEmail());
    }

    @Test
    void testGetPreferenzeUtenteNonTrovato() {
        when(repository.findById("id-inesistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getPreferenze("id-inesistente"));
    }
    
    @Test
    void testLoginCredenzialiErrate() {
        UtenteLoginRequest req = new UtenteLoginRequest("mario@test.it", "wrongpassword");
        when(repository.findByEmailAndPassword("mario@test.it", "wrongpassword"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.login(req));
        verify(repository).findByEmailAndPassword("mario@test.it", "wrongpassword");
    }

    @Test
    void testAggiornaPreferenzeSuccess() {
        when(repository.findById("user123")).thenReturn(Optional.of(mockUtente));
        when(repository.save(any(Utente.class))).thenReturn(mockUtente);

        Map<String, String> preferenze = Map.of("tema", "scuro", "lingua", "it");

        service.aggiornaPreferenze("user123", preferenze);

        verify(repository).findById("user123");
        verify(repository).save(any(Utente.class));
        assertEquals("scuro", mockUtente.getPreferenze().get("tema"));
    }

    @Test
    void testAggiornaPreferenzeUtenteNonTrovato() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        Map<String, String> preferenze = Map.of("tema", "chiaro");

        assertThrows(IllegalArgumentException.class,
                () -> service.aggiornaPreferenze("missing", preferenze));
        verify(repository).findById("missing");
    }


    @Test
    void testGetPreferenzeSuccess() {
        mockUtente.setPreferenze(Map.of("tema", "chiaro"));
        when(repository.findById("user123")).thenReturn(Optional.of(mockUtente));

        Map<String, String> prefs = service.getPreferenze("user123");

        assertEquals("chiaro", prefs.get("tema"));
        verify(repository).findById("user123");
    }

    @Test
    void testDeleteSuccess() {
        service.delete("user123");

        verify(repository).deleteById("user123");
    }

}