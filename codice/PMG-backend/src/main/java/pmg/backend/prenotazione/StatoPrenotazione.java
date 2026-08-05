package pmg.backend.prenotazione;

/**
 * Enum che rappresenta gli stati possibili del ciclo di vita di una prenotazione.
 */
public enum StatoPrenotazione {
    /**
     * Prenotazione creata, in attesa della validazione di ingresso.
     */
    attiva,
    /**
     * Ingresso validato; l'utente si sta dirigendo verso il posto.
     */
    inCorso,
    /**
     * Veicolo parcheggiato e presenza confermata.
     */
    parcheggiato,
    /**
     * Pagamento registrato; uscita ancora da validare.
     */
    pagato,
    /**
     * Uscita completata e prenotazione terminata.
     */
    conclusa,
    /**
     * Tempo massimo di arrivo superato.
     */
    scaduta,
    annullata
}
