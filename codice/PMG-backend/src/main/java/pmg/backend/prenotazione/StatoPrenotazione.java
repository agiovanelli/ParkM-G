package pmg.backend.prenotazione;

public enum StatoPrenotazione {
    ATTIVA,         // Prenotata (attesa entro 10 min)
    IN_CORSO,       // Utente entrato (timer avviato)
    PARCHEGGIATO,   // Utente ha raggiunto e confermato il posto
    PAGATO,         // Saldo effettuato (pronto per uscire, 10 min max)
    CONCLUSA,       // Utente uscito (posto liberato)
    SCADUTA,        // Tempo per l'ingresso esaurito
    ANNULLATA       // Cancellata dall'utente
}