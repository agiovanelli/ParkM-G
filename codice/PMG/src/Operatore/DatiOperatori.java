package Operatore;

import java.io.IOException;

// TODO: Auto-generated Javadoc
/**
 * Interfaccia DatiOperatori.
 */
public interface DatiOperatori {

	/**
	 * Recupera il nome struttura.
	 *
	 * @return il nome struttura
	 */
	public String getNomeStruttura ();
	
	/**
	 * Recupera lo username.
	 *
	 * @return lo username
	 */
	public String getUsername ();
	
	/**
	 * Controlla se esiste l'operatore nel DB.
	 *
	 * @param operatore è l'operatore che è stato creato
	 * @return true, se l'operatore esiste nel DB
	 * @throws IOException segnala che c'è stata una I/O exception.
	 */
	public boolean esisteOperatore(Operatore operatore) throws IOException;
}
