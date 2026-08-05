package pmg.backend.posto;

/**
 * Enum che rappresenta lo stato operativo corrente di un posto auto.
 */
public enum StatoPosto {
    /**
     * Posto disponibile per una nuova prenotazione.
     */
    LIBERO,
    /**
     * Posto bloccato da una prenotazione attiva.
     */
    PRENOTATO,
    OCCUPATO
}
