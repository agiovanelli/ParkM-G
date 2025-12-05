package Utente;

import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Interfaccia DatiUtenti che gestisce la comunicazione tra database e GUI.
 */
public interface DatiUtenti {
	
	/**
	 * Registrazione utente e controllo sul database.
	 *
	 * @return true, se l'utente è nuovo e viene registrato sul database
	 */
	public boolean registrazioneDB();
	
	/**
	 * Login e controllo sul database.
	 *
	 * @return true, se l'utente è già registrato e viene eseguito il login
	 */
	public boolean loginDB();
	
	/**
	 * Eliminazione del record nel database.
	 *
	 * @return true, se l'eliminazione ha avuto successo
	 */
	public boolean deleteDB();
	
	/**
	 * Caricamento delle preferenze selezionate nella GUI nel database.
	 *
	 * @param preferenze Mappa relativa alle preferenze
	 * @return true, se il salvataggio ha avuto successo
	 */
	public boolean selezioneDB(Map<String, String> preferenze);
	
	/**
	 * Controllo credenziali.
	 *
	 * @param email Email dell'utente
	 * @param password Password dell'utente
	 * @return document, non vuoto se esiste il record
	 */
	public Document controlloCredenziali(String email, String password);
	
	/**
	 * Restituisce l'username dell'utente.
	 *
	 * @return Username dell'utente
	 */
	public String getUsername();
	
	/**
	 * Restituisce l'ID dell'utente.
	 *
	 * @return ID dell'utente
	 */
	public ObjectId getId();
	
	/**
	 * Restituisce le preferenze dell'utente.
	 *
	 * @return Preferenze dell'utente
	 */
	public Object getPreferenze();
}
