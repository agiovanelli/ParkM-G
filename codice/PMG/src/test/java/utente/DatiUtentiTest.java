package utente;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Test JUnit 5 per verificare che l'implementazione Utente
 * rispetti il contratto definito dall'interfaccia DatiUtenti.
 *
 * ATTENZIONE: questi test usano il database reale configurato
 * in Connessione.connessioneUtenti().
 */
class DatiUtentiTest {

    // Helper per avere email uniche ed evitare collisioni involontarie
    private String emailUnica(String prefix) {
        return prefix + System.nanoTime() + "@example.com";
    }

    @Test
    void utenteImplementaInterfacciaDatiUtenti() throws IOException {
        DatiUtenti utente = new Utente("Mario", "Rossi", "password123", emailUnica("impl."));

        assertTrue(utente instanceof DatiUtenti,
                "L'oggetto Utente deve essere un'implementazione di DatiUtenti");
    }

    @Test
    void registrazioneDbNuovoUtenteRitornaTrueEImpostaId() throws IOException {
        String email = emailUnica("reg.");
        String password = "password123";

        // Uso il costruttore che NON registra automaticamente
        DatiUtenti utente = new Utente(password, email);

        boolean nuovo = utente.registrazioneDB();

        assertTrue(nuovo, "Per un utente nuovo registrazioneDB deve restituire true");

        ObjectId id = utente.getId();
        assertNotNull(id, "Dopo la registrazione l'ID dell'utente deve essere valorizzato");

        Document trovato = utente.controlloCredenziali(email, password);
        assertNotNull(trovato, "Dopo la registrazione l'utente deve essere presente nel DB");
        assertEquals(email, trovato.getString("email"));
    }

    @Test
    void registrazioneDbUtenteDuplicatoLanciaIllegalStateException() throws IOException {
        String email = emailUnica("dup.");
        String password = "password123";

        // Prima registrazione
        DatiUtenti primo = new Utente(password, email);
        assertTrue(primo.registrazioneDB(), "La prima registrazione deve riuscire");

        // Seconda registrazione con stesse credenziali
        DatiUtenti secondo = new Utente(password, email);

        assertThrows(IllegalStateException.class,
                secondo::registrazioneDB,
                "La registrazione di un utente già esistente deve lanciare IllegalStateException");
    }

    @Test
    void loginDbConCredenzialiCorretteRitornaTrueEImpostaUsernameEId() throws IOException {
        String email = emailUnica("login.ok.");
        String password = "password123";

        // Registro l'utente (costruttore con nome/cognome chiama internamente registrazioneDB())
        new Utente("Mario", "Rossi", password, email);

        // Login tramite interfaccia DatiUtenti
        DatiUtenti login = new Utente(password, email);

        boolean esito = login.loginDB();
        assertTrue(esito, "Il login con credenziali corrette deve restituire true");

        assertNotNull(login.getId(), "Dopo il login l'ID non deve essere nullo");
        assertEquals("Mario.Rossi", login.getUsername(),
                "Lo username deve essere quello salvato in fase di registrazione (nome.cognome)");
    }

    @Test
    void loginDbConPasswordErrataLanciaIllegalArgumentException() throws IOException {
        String email = emailUnica("login.fail.");
        String passwordCorretta = "passwordCorretta";
        String passwordErrata = "passwordErrata";

        // Registro l'utente con la password corretta
        new Utente("Mario", "Rossi", passwordCorretta, email);

        // Provo il login con password sbagliata
        DatiUtenti login = new Utente(passwordErrata, email);

        assertThrows(IllegalArgumentException.class,
                login::loginDB,
                "Login con password errata deve lanciare IllegalArgumentException");
    }

    @Test
    void deleteDbRimuoveLUtenteDalDatabase() throws IOException {
        String email = emailUnica("del.");
        String password = "password123";

        DatiUtenti utente = new Utente(password, email);
        assertTrue(utente.registrazioneDB(), "Registrazione iniziale deve andare a buon fine");

        boolean eliminato = utente.deleteDB();
        assertTrue(eliminato, "deleteDB deve restituire true se l'utente viene rimosso");

        Document trovato = utente.controlloCredenziali(email, password);
        assertNull(trovato, "Dopo la delete l'utente non deve più essere presente nel DB");
    }

    @Test
    void selezioneDbSalvaLePreferenzeEGetPreferenzeLeRestituisce() throws IOException {
        String email = emailUnica("pref.");
        String password = "password123";

        DatiUtenti utente = new Utente(password, email);
        assertTrue(utente.registrazioneDB(), "Registrazione iniziale deve andare a buon fine");

        Map<String, String> preferenze = Map.of(
                "zona", "centro",
                "cucina", "italiana"
        );

        boolean aggiornate = utente.selezioneDB(preferenze);
        assertTrue(aggiornate, "selezioneDB deve restituire true se il documento è stato aggiornato");

        Object prefsObj = utente.getPreferenze();
        assertNotNull(prefsObj, "Le preferenze non devono essere nulle dopo l'aggiornamento");
        assertTrue(prefsObj instanceof Document, "Le preferenze dovrebbero essere un Document BSON");

        Document prefs = (Document) prefsObj;
        assertEquals("centro", prefs.getString("zona"));
        assertEquals("italiana", prefs.getString("cucina"));
    }

    @Test
    void getIdRestituisceValoreNonNulloDopoRegistrazione() throws IOException {
        String email = emailUnica("id.");
        String password = "password123";

        DatiUtenti utente = new Utente(password, email);
        utente.registrazioneDB();

        ObjectId id = utente.getId();
        assertNotNull(id, "Dopo la registrazione l'ID deve essere valorizzato");
    }
}
