package Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import Database.Connessione;

/**
 * Classe Utente.
 */
public class Utente implements DatiUtenti, GestioneUtenti{
	
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
	
	/** Preferenze selezionate dall'utente. */
	private Map<String, String> preferenze;
	
	/** Controlla se l'utente è stato appena creato o esiste già. */
	public boolean nuovoUser = false;
	
	private static Utente utenteLoggato;

	/** Collezione di documenti della collection utenti in collegamento con il database. */
	private final MongoCollection<Document> utenti;
	
	/**
	 * Registrazione dell'utente.
	 *
	 * @param nome Nome dell'utente
	 * @param cognome Cognome dell'utente
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @throws Exception Eccezione che viene lanciata in caso di errore
	 */
	public Utente(String nome, String cognome, String password, String email) throws Exception{
		this.nome = nome;
		this.cognome = cognome;
		this.password = password;
		this.email = email;
		
		this.username = nome + "." + cognome;
		
		Connessione connection = new Connessione();
        this.utenti = connection.connessioneUtenti();
		
		registrazioneDB();
	}
	
	/**
	 * Login dell'utente.
	 *
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @throws Exception Eccezione che viene lanciata in caso di errore
	 */
	public Utente(String password, String email) throws Exception{
		this.password = password;
		this.email = email;
		
		Connessione connection = new Connessione();
        this.utenti = connection.connessioneUtenti();
        
        loginDB();
	}

	/**
	 * Registrazione utente e controllo sul database.
	 *
	 * @return true, se l'utente è nuovo e viene registrato sul database
	 */
	@Override
	public boolean registrazioneDB() {
		// Controlla se il database ha già questo record
		if(controlloCredenziali(this.email, this.password) != null) {
			System.out.println("Utente già registrato");
			return this.nuovoUser = false;
		}
		
		// Altrimenti lo salva nel db
        Document nuovo = new Document("nome", this.nome)
        		.append("cognome", this.cognome)
        		.append("password", this.password)
                .append("email", this.email)
                .append("username", this.username);

        utenti.insertOne(nuovo);
        
        // Recupero l'id del documento dal database
        this.id = nuovo.getObjectId("_id");
        
        return this.nuovoUser = true;
	}

	/**
	 * Login e controllo sul database.
	 *
	 * @return true, se l'utente è già registrato e viene eseguito il login
	 */
	@Override
	public boolean loginDB() {
		// Controlla se il database ha già questo record
		if(controlloCredenziali(this.email, this.password) == null) {
			System.out.println("Utente non registrato");
			return false;
		}
		
		// Recupero l'id del documento dal database
		this.id = controlloCredenziali(this.email, this.password).getObjectId("_id");
		this.username = controlloCredenziali(this.email, this.password).getString("username");
		
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
		Document eliminazione = utenti.findOneAndDelete(Filters.and(
				Filters.eq("email", this.email),
				Filters.eq("password", this.password)));
		
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
		// TODO Azione di salvataggio delle preferenze nel db
		
		List<Bson> updates = new ArrayList<>();
		preferenze.forEach((k, v) -> updates.add(Updates.set("preferenze." + k, v)));
		
		UpdateResult update = utenti.updateOne(
			    Filters.eq("_id", this.id),
			    Updates.combine(updates)
			);
				
		return true;
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
	 * @throws Exception Eccezione che viene lanciata in caso di errore
	 */
	@Override
	public Utente registrazione(String nome, String cognome, String password, String email) throws Exception{
		Utente newUser = new Utente(nome, cognome, password, email);
		registrazioneDB();
		return newUser;
	}

	/**
	 * Login.
	 *
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @return Utente loggato
	 * @throws Exception Eccezione che viene lanciata in caso di errore
	 */
	@Override
	public Utente login(String password, String email) throws Exception{
		Utente user = new Utente(password, email);
		return user;
	}

	/**
	 * Logout.
	 */
	@Override
	public void logout() {
		 System.out.println("Logout eseguito correttamente");
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
	 */
	@Override
	public Document controlloCredenziali(String email, String password) {
		Document esistente = utenti.find(Filters.and(
				Filters.eq("email", email),
				Filters.eq("password", password)
				)).first();
		
		return esistente;
	}
}
