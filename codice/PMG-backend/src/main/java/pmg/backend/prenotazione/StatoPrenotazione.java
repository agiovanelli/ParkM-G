package pmg.backend.prenotazione;

public enum StatoPrenotazione {
    attiva,         // Prenotata (attesa entro 10 min)
    inCorso,       // Utente entrato (timer avviato)
    parcheggiato,   // Utente ha raggiunto e confermato il posto
    pagato,         // Saldo effettuato (pronto per uscire, 10 min max)
    conclusa,       // Utente uscito (posto liberato)
    scaduta,        // Tempo per l'ingresso esaurito
    annullata       // Cancellata dall'utente
}