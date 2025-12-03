package Utente;

import java.util.Map;

/**
 * Interfaccia GestioneUtenti che gestisce l'interazione con la GUI.
 */
public interface GestioneUtenti{
	
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
	public Utente registrazione(String nome, String cognome, String password, String email) throws Exception;
	
	/**
	 * Login.
	 *
	 * @param password Password dell'utente
	 * @param email Email dell'utente
	 * @return Utente loggato
	 * @throws Exception Eccezione che viene lanciata in caso di errore
	 */
	public Utente login(String password, String email) throws Exception;
	
	/**
	 * Logout.
	 */
	public void logout();
	
	/**
	 * Imposta la selezione delle preferenze.
	 *
	 * @param preferenze Mappa delle preferenze
	 */
	public void setSelezione(Map<String, String> preferenze);
}
