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
        // GIVEN
        UtenteRegisterRequest req = new UtenteRegisterRequest("Mario", "Rossi", "mario@test.it", "password123");
        when(repository.existsByEmail("mario@test.it")).thenReturn(false);
        when(repository.save(any(Utente.class))).thenReturn(mockUtente);

        // WHEN
        UtenteResponse resp = service.registrazione(req);

        // THEN
        assertNotNull(resp);
        assertEquals("mario.rossi", resp.getUsername()); // Verifica generazione username
        verify(repository).save(any(Utente.class));
    }

    @Test
    void testRegistrazioneEmailGiaEsistente() {
        // GIVEN
        UtenteRegisterRequest req = new UtenteRegisterRequest("Mario", "Rossi", "mario@test.it", "password123");
        when(repository.existsByEmail("mario@test.it")).thenReturn(true);

        // WHEN & THEN
        assertThrows(IllegalStateException.class, () -> service.registrazione(req));
    }

    @Test
    void testLoginSuccess() {
        // GIVEN
        UtenteLoginRequest req = new UtenteLoginRequest("mario@test.it", "password123");
        when(repository.findByEmailAndPassword("mario@test.it", "password123"))
                .thenReturn(Optional.of(mockUtente));

        // WHEN
        UtenteResponse resp = service.login(req);

        // THEN
        assertEquals("user123", resp.getId());
        assertEquals("mario@test.it", resp.getEmail());
    }

    @Test
    void testGetPreferenzeUtenteNonTrovato() {
        // GIVEN
        when(repository.findById("id-inesistente")).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> service.getPreferenze("id-inesistente"));
    }
    
    @Test
    void testLoginCredenzialiErrate() {
        // GIVEN
        UtenteLoginRequest req = new UtenteLoginRequest("mario@test.it", "wrongpassword");
        when(repository.findByEmailAndPassword("mario@test.it", "wrongpassword"))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> service.login(req));
        verify(repository).findByEmailAndPassword("mario@test.it", "wrongpassword");
    }

    @Test
    void testAggiornaPreferenzeSuccess() {
        // GIVEN
        when(repository.findById("user123")).thenReturn(Optional.of(mockUtente));
        when(repository.save(any(Utente.class))).thenReturn(mockUtente);

        Map<String, String> preferenze = Map.of("tema", "scuro", "lingua", "it");

        // WHEN
        service.aggiornaPreferenze("user123", preferenze);

        // THEN
        verify(repository).findById("user123");
        verify(repository).save(any(Utente.class));
        assertEquals("scuro", mockUtente.getPreferenze().get("tema"));
    }

    @Test
    void testAggiornaPreferenzeUtenteNonTrovato() {
        // GIVEN
        when(repository.findById("missing")).thenReturn(Optional.empty());
        Map<String, String> preferenze = Map.of("tema", "chiaro");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> service.aggiornaPreferenze("missing", preferenze));
        verify(repository).findById("missing");
    }


    @Test
    void testGetPreferenzeSuccess() {
        // GIVEN
        mockUtente.setPreferenze(Map.of("tema", "chiaro"));
        when(repository.findById("user123")).thenReturn(Optional.of(mockUtente));

        // WHEN
        Map<String, String> prefs = service.getPreferenze("user123");

        // THEN
        assertEquals("chiaro", prefs.get("tema"));
        verify(repository).findById("user123");
    }

    @Test
    void testDeleteSuccess() {
        // WHEN
        service.delete("user123");

        // THEN
        verify(repository).deleteById("user123");
    }

}