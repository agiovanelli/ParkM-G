package pmg.backend.log;

/**
 * Enum che rappresenta le categorie dei log.
 *
 * Permette di distinguere i log in base alla tipologia
 * dell'evento registrato.
 */
public enum LogCategoria {
	
	/** Log relativo a un evento ordinario. */
	EVENTO, 
	
    /** Log relativo a un allarme. */
    ALLARME, 
    
    /** Log relativo allo storico degli eventi. */
    HISTORY
}