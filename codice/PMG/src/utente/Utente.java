package utente;

import java.io.IOException; 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger; // Logger
import org.slf4j.LoggerFactory; // Logger Factory

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import database.Connessione;

/**
 * Classe Utente.
 */
public class Utente implements DatiUtenti, GestioneUtenti{
	
    // 1. RISOLUZIONE DUPLICAZIONE LITERAL: Costanti per i nomi dei campi DB
    private static final String FIELD_NOME = "nome";
    private static final String FIELD_COGNOME = "cognome";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_USERNAME = "username";

    // 2. RISOLUZIONE System.out: Dichiarazione del Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(Utente.class);

	/** Nome dell'utente. */
	private String nome;
	
	/** Cognome dell'utente. */
	private String cognome;
	
	/** Username dell'utente. */
	private String username;
	
	/** Password dell'utente. */
	private String password;
	
	/** Email dell'utente. */
	private String email;
	
	/** ID dell'utente. */
	private ObjectId id;
	
	/** Preferenze selezionate dall'utente. 
	private Map<String, String> preferenze;*/
	
	/** Controlla se l'utente è stato appena creato o esiste già. */
	public boolean nuovoUser = false;
	
	/** Controlla se l'utente è loggato o meno. 
	private static Utente utenteLoggato;*/

	/** Collezione di documenti della collection utenti in collegamento con il database. */
	private final MongoCollection<Document> utenti;
	
	/**
	 * Registrazione dell'utente.
	 *
	 * @param nome Nome dell'utente
	 * @param cognome Cognome dell'utente
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @throws IOException in caso di errore di connessione.
	 */
	
	
	
	public Utente(String nome, String cognome, String password, String email) throws IOException { // 3. Eccezione specifica
		this.nome = nome;
		this.cognome = cognome;
		this.password = password;
		this.email = email;
		
		this.username = nome + "." + cognome;
		
		this.utenti = Connessione.connessioneUtenti();

        // Gestione dell'eccezione (necessario perché registrazioneDB ora lancia IOException)
        try {
            registrazioneDB();
        } catch (IllegalStateException e) {
            // L'utente esiste già, non è un errore critico di costruzione
            LOGGER.warn("Costruttore: L'utente esiste già", e);
        }
	}
	
	/**
	 * Login dell'utente.
	 *
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @throws IOException in caso di errore di connessione.
	 */
	public Utente(String password, String email) throws IOException { // 3. Eccezione specifica
		this.password = password;
		this.email = email;
		
		this.utenti = Connessione.connessioneUtenti();

		loginDB();
	}

	/**
	 * Registrazione utente e controllo sul database.
	 *
	 * @return true, se l'utente è nuovo e viene registrato sul database
	 * @throws IOException in caso di errore di connessione al database.
	 * @throws IllegalStateException in caso l'utente tenti di registrarsi con una email già esistente.
	 */
	@Override
	public boolean registrazioneDB() throws IOException, IllegalStateException {
	    // Controlla se il database ha già questo record
	    if(controlloCredenziali(this.email, this.password) != null) {
	        // RISOLUZIONE System.out: Uso del Logger
	        LOGGER.warn("Registrazione fallita: Utente con credenziali già registrate");
	        this.nuovoUser = false;
	        throw new IllegalStateException("L'utente con questa email e password è già registrato.");
	    }
	    
	    // Altrimenti lo salva nel db
	    // RISOLUZIONE DUPLICAZIONE LITERAL: Uso delle costanti
	    Document nuovo = new Document(FIELD_NOME, this.nome)
	            .append(FIELD_COGNOME, this.cognome)
	            .append(FIELD_PASSWORD, this.password)
	            .append(FIELD_EMAIL, this.email)
	            .append(FIELD_USERNAME, this.username);

	    utenti.insertOne(nuovo);
	    
	    // Recupero l'id del documento dal database
	    this.id = nuovo.getObjectId("_id"); // <--- QUESTA RIGA È STATA VERIFICATA
	    
	    LOGGER.info("Nuovo utente registrato con successo: {}", this.email);
	    this.nuovoUser = true;
	    return this.nuovoUser;
	}

	/**
	 * Login e controllo sul database.
	 *
	 * @return true, se l'utente è già registrato e viene eseguito il login
     * @throws IOException in caso di errore di connessione.
     * @throws IllegalArgumentException in caso di credenziali errate.
	 */
	@Override
	public boolean loginDB() throws IOException, IllegalArgumentException {
		// Controlla se il database ha già questo record
		if(controlloCredenziali(this.email, this.password) == null) {
			// 2. RISOLUZIONE System.out: Uso del Logger
			LOGGER.warn("Login fallito: Utente non registrato o credenziali errate per: {}", this.email);
            throw new IllegalArgumentException("Credenziali non valide.");
		}
		
		// Recupero l'id del documento dal database
		Document utenteDoc = controlloCredenziali(this.email, this.password);
        this.id = utenteDoc.getObjectId("_id");
		this.username = utenteDoc.getString(FIELD_USERNAME); // 1. Uso della costante
		
        LOGGER.info("Login eseguito correttamente per l'utente: {}", this.email);
		return true;
	}
	
	/**
	 * Eliminazione del record nel database.
	 *
	 * @return true, se l'eliminazione ha avuto successo
	 */
	@Override
	public boolean deleteDB() {
		// Trova il record con stesso id e aggiorna il database eliminandolo
        // 1. Uso della costante
		Document eliminazione = utenti.findOneAndDelete(Filters.and(
				Filters.eq(FIELD_EMAIL, this.email),
				Filters.eq(FIELD_PASSWORD, this.password)));
		
		return eliminazione != null;
	}

	/**
	 * Caricamento delle preferenze selezionate nella GUI nel database.
	 *
	 * @param preferenze Mappa relativa alle preferenze
	 * @return true, se il salvataggio ha avuto successo
	 */
	@Override
	public boolean selezioneDB(Map<String, String> preferenze) {
	    List<Bson> updates = new ArrayList<>();
	    
	    // Si usa il forEach per costruire la lista di Bson updates
	    preferenze.forEach((k, v) -> updates.add(Updates.set("preferenze." + k, v)));
	    
	    UpdateResult update = utenti.updateOne(
	            Filters.eq("_id", this.id),
	            Updates.combine(updates)
	        );
	        
	  
	    return update.getModifiedCount() == 1;
	}

	/**
	 * Restituisce l'username dell'utente.
	 *
	 * @return Username dell'utente
	 */
	@Override
	public String getUsername() {
		return this.username;
	}

	/**
	 * Restituisce l'ID dell'utente.
	 *
	 * @return ID dell'utente
	 */
	@Override
	public ObjectId getId() {
		return this.id;
	}

	/**
	 * Restituisce le preferenze dell'utente.
	 *
	 * @return Preferenze dell'utente
	 */
	@Override
	public Object getPreferenze() {
		Document esistente = utenti.find(Filters.eq("_id", this.id)).first();
		
		return esistente.get("preferenze");
	}

	/**
	 * Registrazione.
	 *
	 * @param nome Nome dell'utente
	 * @param cognome Cognome dell'utente
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @return Utente registrato
	 * @throws IOException in caso di errore di connessione.
	 * @throws IllegalStateException in caso l'utente tenti di registrarsi con una email già esistente.
	 */
	@Override
	public Utente registrazione(String nome, String cognome, String password, String email) throws IOException, IllegalStateException { // 3. Eccezione specifica
		return new Utente(nome, cognome, password, email);
        
		
	}

	/**
	 * Login.
	 *
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @return Utente loggato
	 * @throws IOException in caso di errore di connessione.
	 * @throws IllegalArgumentException in caso di credenziali errate.
	 */
	@Override
	public Utente login(String password, String email) throws IOException, IllegalArgumentException { // 3. Eccezione specifica
		return new Utente(password, email);
		
	}

	/**
	 * Logout.
	 */
	@Override
	public void logout() {
        // 2. RISOLUZIONE System.out: Uso del Logger
		LOGGER.info("Logout eseguito correttamente per l'utente: {}", this.email);
	}
	
	/**
	 * Imposta la selezione delle preferenze.
	 *
	 * @param preferenze Mappa delle preferenze
	 */
	@Override
	public void setSelezione(Map<String, String> preferenze) {
		selezioneDB(preferenze);
	}

	/**
	 * Controllo credenziali.
	 *
	 * @param email Email dell'utente
	 * @param password Password dell'utente
	 * @return document, non vuoto se esiste il record
     * @throws IOException in caso di errore di connessione al database.
	 */
	@Override
	public Document controlloCredenziali(String email, String password) throws IOException {
		
        
		return utenti.find(Filters.and(
				Filters.eq(FIELD_EMAIL, email),
				Filters.eq(FIELD_PASSWORD, password)
				)).first();
		
	}
}