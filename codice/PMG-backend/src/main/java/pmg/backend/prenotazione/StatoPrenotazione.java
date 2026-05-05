package pmg.backend.prenotazione;

/**
 * Enum che rappresenta le tipologie di stato delle prenotazioni.
 *
 * Permette di classificare una prenotazione in base
 * allo stato in cui si trova.
 */
public enum StatoPrenotazione {
	
	/** Prenotazione attiva, arrivo al parcheggio atteso entro un limite. */
    attiva,
    
    /** Utente entrato nel parcheggio, si dirige verso il posto assegnato. */
    inCorso,
    
    /** Utente ha parcheggiato e confermato il posto. */
    parcheggiato,
    
    /** Prenotazione pagata, l'utente ha un limite di tempo entro qui può uscire dal parcheggio. */
    pagato,
    
    /** Prenotazione conclusa, utente uscito dal parcheggio con successo. */
    conclusa,
    
    /** Prenotazione scaduta, l'utente ha esaurito il tempo massimo entro il quale poteva effettuare l'accesso. */
    scaduta,
    
    /** Prenotazione annullata, l'utente ha cancellato la prenotazione. */
    annullata
}