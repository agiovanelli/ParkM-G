package pmg.backend.log;

/**
 * Enum che rappresenta i livelli o le tipologie di severità dei log.
 *
 * Permette di classificare un log in base alla gravità
 * o al contesto dell'evento registrato.
 */
public enum LogSeverità {
	
	/** Log relativo a una situazione critica. */
	CRITICO, 
	
    /** Log relativo a una situazione che richiede attenzione. */
    ATTENZIONE, 
    
    /** Log relativo a un controllo o una verifica. */
    CONTROLLO,
	
	/** Log relativo a un evento di pagamento. */
	PAGAMENTO, 
	
    /** Log relativo a un veicolo. */
    VEICOLO, 
    
    /** Log informativo. */
    INFO,
	
	/** Log relativo a una situazione risolta. */
	RISOLTO
}