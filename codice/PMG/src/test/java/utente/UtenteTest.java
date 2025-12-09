package utente;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import org.junit.jupiter.api.Test;
import org.bson.Document;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import database.Connessione;
import utente.Utente;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;

public class UtenteTest {

    // --- Configurazione per i Test di Integrazione ---

    // Dati per l'utente permanente usato nei test di Login
    private static final String NOME_TEST = "TestNome";
    private static final String COGNOME_TEST = "TestCognome";
    private static final String EMAIL_ESISTENTE = "login.test@parkmg.it";
    private static final String PASSWORD_ESISTENTE = "TestPwd2025";
    
    // Email univoca per i test di registrazione
    private final String EMAIL_UNICA = "newuser_" + System.nanoTime() + "@unique.it";

    // Riferimento alla collezione per pulizia e setup
    private static MongoCollection<Document> utentiCollection;
    
    // Costanti della classe Utente (replicare per testing)
    private static final String FIELD_NOME = "nome";
    private static final String FIELD_COGNOME = "cognome";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_USERNAME = "username";


    /**
     * Setup Eseguito una sola volta prima di tutti i test. 
     * Assicura che la collezione di test sia pronta e contenga l'utente per il login.
     */
    @BeforeAll
    static void setupDatabase() throws IOException {
        // Assume che Connessione.connessioneUtenti() funzioni correttamente
        utentiCollection = Connessione.connessioneUtenti();

        // 1. Rimuove qualsiasi residuo dell'utente di test permanente
        utentiCollection.deleteOne(Filters.eq(FIELD_EMAIL, EMAIL_ESISTENTE));

        // 2. Inserisce l'utente di test permanente per i test di Login
        Document utentePermanente = new Document(FIELD_NOME, NOME_TEST)
                .append(FIELD_COGNOME, COGNOME_TEST)
                .append(FIELD_PASSWORD, PASSWORD_ESISTENTE)
                .append(FIELD_EMAIL, EMAIL_ESISTENTE)
                .append(FIELD_USERNAME, NOME_TEST + "." + COGNOME_TEST);
        
        utentiCollection.insertOne(utentePermanente);
    }
    
    /**
     * Pulizia Eseguita dopo tutti i test. Rimuove l'utente temporaneo e l'utente permanente.
     */
    @AfterAll
    static void cleanUpDatabase() {
        if (utentiCollection != null) {
            // Rimuove l'utente di test permanente
            utentiCollection.deleteOne(Filters.eq(FIELD_EMAIL, EMAIL_ESISTENTE));
            // Rimuove tutti gli utenti con l'email di test temporaneo (per sicurezza)
            utentiCollection.deleteMany(Filters.regex(FIELD_EMAIL, "^newuser_.*"));
        }
    }
    
    // --- Test sui Costruttori ---

    // A. TEST: Costruttore Registrazione (Successo)
    @Test
    void testCostruttoreRegistrazione_Successo() throws IOException {
        // Il costruttore chiama registrazioneDB() internamente.
        // Usa un'email unica per garantire che il test sia pulito.
        Utente u = new Utente("Nuovo", "User", "Pswd123!", EMAIL_UNICA);

        assertTrue(u.nuovoUser, "Dopo la registrazione il flag 'nuovoUser' deve essere true.");
        assertNotNull(u.getId(), "L'ID non deve essere nullo dopo il successo della registrazione.");
        
        // Pulizia manuale
        utentiCollection.deleteOne(Filters.eq(FIELD_EMAIL, EMAIL_UNICA));
    }

    // B. TEST: Costruttore Registrazione (Utente Esistente)
    @Test
    void testCostruttoreRegistrazione_UtenteEsistente() throws IOException {
        // Il costruttore chiama registrazioneDB() e 'cattura' l'IllegalStateException
        // impostando solo nuovoUser = false e loggando un warning.
        
        Utente u = new Utente(NOME_TEST, COGNOME_TEST, PASSWORD_ESISTENTE, EMAIL_ESISTENTE);
        
        assertFalse(u.nuovoUser, "Il flag 'nuovoUser' deve essere false se l'utente esiste già.");
        // Non possiamo asserire che l'ID sia stato impostato correttamente in questo costruttore
        // perché il codice non esegue il login se l'utente esiste.
    }

    // C. TEST: Costruttore Login
    @Test
    void testCostruttoreLogin_Successo() throws IOException {
        // Il costruttore chiama loginDB() internamente.
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE);
        
        // Verifica che il login sia avvenuto (loginDB() è implicito)
        assertNotNull(u.getId(), "L'ID deve essere impostato dopo un login riuscito.");
        assertEquals(NOME_TEST + "." + COGNOME_TEST, u.getUsername(), "L'username deve essere recuperato correttamente.");
    }
    
    // --- Test sui Metodi di Interazione con il Database ---

    // D. TEST: registrazioneDB (Successo)
    @Test
    void testRegistrazioneDB_Successo() throws Exception {
        String emailUnico = "regdb_ok_" + System.nanoTime() + "@parkmg.it";
        Utente u = new Utente("Reg", "DB", "RegPwd1!", emailUnico);
        
        // Resetta lo stato (l'utente è stato creato nel costruttore, ma riproviamo il metodo per copertura)
        utentiCollection.deleteOne(Filters.eq(FIELD_EMAIL, emailUnico));
        u.nuovoUser = false; // resetta il flag
        
        // Esecuzione e verifica del successo
        assertTrue(u.registrazioneDB(), "RegistrazioneDB deve restituire true.");
        
        // Verifica indiretta nel DB
        assertNotNull(utentiCollection.find(Filters.eq(FIELD_EMAIL, emailUnico)).first(), 
                      "Il documento deve essere presente nel database.");
    }

    // E. TEST: registrazioneDB (Utente Esistente -> Eccezione)
    @Test
    void testRegistrazioneDB_UtenteEsistente() throws IOException {
        // L'utente esiste grazie a @BeforeAll
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE);
        
        // Verifichiamo che venga lanciata l'IllegalStateException come previsto
        assertThrows(IllegalStateException.class, u::registrazioneDB, 
                     "La registrazione di un utente esistente deve lanciare IllegalStateException.");
        
        assertFalse(u.nuovoUser, "Il flag 'nuovoUser' deve essere false se la registrazione fallisce.");
    }

    // F. TEST: loginDB (Successo)
    @Test
    void testLoginDB_Successo() throws IOException {
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE);
        
        // loginDB è chiamato dal costruttore, quindi basta controllare l'ID
        assertNotNull(u.getId(), "L'ID deve essere recuperato dopo il login.");
        assertTrue(u.loginDB(), "Il loginDB deve restituire true.");
    }

    // G. TEST: loginDB (Fallimento -> Eccezione Credenziali Errate)
    @Test
    void testLoginDB_CredenzialiErrate() throws IOException {
        // Creiamo l'utente con credenziali errate (password sbagliata)
        Utente u = new Utente("PasswordSbagliata", EMAIL_ESISTENTE);
        
        // Verifichiamo che venga lanciata l'IllegalArgumentException
        assertThrows(IllegalArgumentException.class, u::loginDB, 
                     "Il login con password errata deve lanciare IllegalArgumentException.");
    }

    // H. TEST: deleteDB
    @Test
    void testDeleteDB() throws Exception {
        String emailDaEliminare = "delete.me_" + System.nanoTime() + "@parkmg.it";
        
        // 1. Registra un utente temporaneo
        Utente u = new Utente("Elimina", "Me", "delPwd1", emailDaEliminare);
        
        // 2. Esegui l'eliminazione
        assertTrue(u.deleteDB(), "DeleteDB deve restituire true per l'eliminazione riuscita.");
        
        // 3. Verifica nel DB che non esista più
        Document eliminato = utentiCollection.find(Filters.eq(FIELD_EMAIL, emailDaEliminare)).first();
        assertNull(eliminato, "Il documento non deve più esistere nel database dopo deleteDB.");
    }
    
    // I. TEST: selezioneDB (Salvataggio Preferenze)
    @Test
    void testSelezioneDB_Successo() throws IOException {
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE); // Login per ottenere l'ID
        
        Map<String, String> preferenze = new HashMap<>();
        preferenze.put("tema", "dark");
        preferenze.put("lingua", "italiano");

        // Esegue il salvataggio
        assertTrue(u.selezioneDB(preferenze), "Il salvataggio delle preferenze deve avere successo.");
        
        // Verifica il recupero e il contenuto
        Map<String, String> preferenzeSalvato = (Map<String, String>) u.getPreferenze();
        assertEquals("dark", preferenzeSalvato.get("tema"), "La preferenza 'tema' deve essere stata salvata.");
    }

    // J. TEST: getPreferenze
    @Test
    void testGetPreferenze() throws IOException {
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE); // Login
        
        // Assicurati che le preferenze siano state salvate prima
        Map<String, String> pref = new HashMap<>();
        pref.put("testKey", "testValue");
        u.selezioneDB(pref);
        
        Map<String, String> risultato = (Map<String, String>) u.getPreferenze();
        assertNotNull(risultato, "GetPreferenze non deve restituire null dopo il salvataggio.");
        assertEquals("testValue", risultato.get("testKey"), "Il valore della chiave deve corrispondere.");
    }

    // K. TEST: Altri Getter e Metodi Wrapper
    @Test
    void testGetterAndWrappers() throws IOException {
        Utente u = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE);

        // Test Getter
        assertNotNull(u.getId());
        assertEquals(NOME_TEST + "." + COGNOME_TEST, u.getUsername());
        
        // Test Logout (copre solo la riga del logger)
        u.logout(); 
        
        // Test Registrazione wrapper
        Utente uReg = u.registrazione("Wrapper", "Test", "wPwd1!", "wrapper.test@parkmg.it");
        assertNotNull(uReg);
        utentiCollection.deleteOne(Filters.eq(FIELD_EMAIL, "wrapper.test@parkmg.it"));
        
        // Test Login wrapper
        Utente uLogin = u.login(PASSWORD_ESISTENTE, EMAIL_ESISTENTE);
        assertNotNull(uLogin);
    }
    
    // L. TEST: Controllo Credenziali (Utente non esistente)
    @Test
    void testControlloCredenziali_NonTrovato() throws IOException {
        Document risultato = new Utente(PASSWORD_ESISTENTE, EMAIL_ESISTENTE).controlloCredenziali("non.esiste@mail.com", "anypwd");
        assertNull(risultato, "Il controllo credenziali deve restituire null se l'utente non è trovato.");
    }
}