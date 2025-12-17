package utente;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;

/**
 * Test JUnit 5 per la classe Utente.
 *
 * ATTENZIONE:
 * Questi test usano il vero database configurato in Connessione.connessioneUtenti().
 * Assicurati che il DB di test sia attivo e raggiungibile.
 */
class UtenteTest {

    @Test
    void testRegistrazioneDbInserisceNuovoUtente() throws Exception {
        String email = "test.registrazione." + System.nanoTime() + "@example.com";
        String password = "password123";

        // Uso il costruttore (password, email) che NON registra automaticamente
        Utente utente = new Utente(password, email);

        boolean nuovo = utente.registrazioneDB();

        assertTrue(nuovo, "L'utente dovrebbe essere riconosciuto come nuovo");
        Document trovato = utente.controlloCredenziali(email, password);
        assertNotNull(trovato, "Dopo la registrazione l'utente deve esistere nel DB");
        assertEquals(email, trovato.getString("email"));
    }

    @Test
    void testRegistrazioneDbUtenteDuplicatoLanciaEccezione() throws Exception {
        String email = "test.dup." + System.nanoTime() + "@example.com";
        String password = "password123";

        Utente primo = new Utente(password, email);
        assertTrue(primo.registrazioneDB(), "La prima registrazione deve andare a buon fine");

        Utente secondo = new Utente(password, email);

        assertThrows(IllegalStateException.class, () -> {
            secondo.registrazioneDB();
        }, "La seconda registrazione con stesse credenziali deve lanciare IllegalStateException");
    }

    @Test
    void testLoginDbConCredenzialiCorrette() throws Exception {
        String email = "test.login." + System.nanoTime() + "@example.com";
        String password = "password123";

        // Questo costruttore chiama internamente registrazioneDB() (in un try/catch)
        Utente registrato = new Utente("Mario", "Rossi", password, email);

        // Per il login uso l'altro costruttore (password, email)
        Utente login = new Utente(password, email);

        assertTrue(login.loginDB(), "Il login con credenziali corrette deve restituire true");
        assertNotNull(login.getId(), "Dopo il login l'id dell'utente deve essere valorizzato");
        assertEquals("Mario.Rossi", login.getUsername(), "Lo username deve essere recuperato dal DB");
    }

    @Test
    void testLoginDbConCredenzialiErrateLanciaEccezione() throws Exception {
        String email = "test.login.fail." + System.nanoTime() + "@example.com";
        String passwordRegistrato = "passwordRegistrato";
        String passwordErrata = "passwordErrata";

        // Registro un utente con una certa password
        Utente registrato = new Utente("Mario", "Rossi", passwordRegistrato, email);

        // Provo a loggarmi con la password sbagliata
        Utente login = new Utente(passwordErrata, email);

        assertThrows(IllegalArgumentException.class, () -> {
            login.loginDB();
        }, "Login con password errata deve lanciare IllegalArgumentException");
    }

    @Test
    void testDeleteDbEliminaUtente() throws Exception {
        String email = "test.delete." + System.nanoTime() + "@example.com";
        String password = "password123";

        Utente utente = new Utente(password, email);
        assertTrue(utente.registrazioneDB(), "Registrazione iniziale deve andare a buon fine");

        assertTrue(utente.deleteDB(), "deleteDB deve restituire true se l'utente viene eliminato");

        Document trovato = utente.controlloCredenziali(email, password);
        assertNull(trovato, "Dopo la delete l'utente non deve più esistere nel DB");
    }

    @Test
    void testSelezioneDbESalvataggioPreferenze() throws Exception {
        String email = "test.pref." + System.nanoTime() + "@example.com";
        String password = "password123";

        Utente utente = new Utente("Mario", "Rossi", password, email);
        // Il costruttore con nome/cognome registra l'utente e valorizza this.id

        Map<String, String> preferenze = Map.of(
                "zona", "centro",
                "cucina", "italiana"
        );

        assertTrue(utente.selezioneDB(preferenze), "Il salvataggio delle preferenze deve andare a buon fine");

        Object prefsObj = utente.getPreferenze();
        assertNotNull(prefsObj, "Le preferenze salvate non devono essere nulle");
        assertTrue(prefsObj instanceof Document, "Le preferenze dovrebbero essere un Document BSON");

        Document prefs = (Document) prefsObj;
        assertEquals("centro", prefs.getString("zona"));
        assertEquals("italiana", prefs.getString("cucina"));
    }

    @Test
    void testGetUsernameDopoCostruttore() throws Exception {
        String email = "test.username." + System.nanoTime() + "@example.com";
        String password = "password123";

        Utente utente = new Utente("Mario", "Rossi", password, email);

        assertEquals("Mario.Rossi", utente.getUsername(),
                "Lo username deve essere nel formato nome.cognome");
    }

    @Test
    void testLogoutNonLanciaEccezioni() throws Exception {
        String email = "test.logout." + System.nanoTime() + "@example.com";
        String password = "password123";

        Utente utente = new Utente("Mario", "Rossi", password, email);

        assertDoesNotThrow(utente::logout, "logout non dovrebbe lanciare eccezioni");
    }
}
