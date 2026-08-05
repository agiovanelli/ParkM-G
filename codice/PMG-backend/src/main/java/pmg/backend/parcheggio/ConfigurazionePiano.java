package pmg.backend.parcheggio;

import java.util.ArrayList;
import java.util.List;

import pmg.backend.posto.Posto;

/**
 * Rappresenta la configurazione embedded di un singolo piano del parcheggio.
 *
 * Contiene il numero identificativo del piano, la capacità configurata e
 * l'elenco dei posti memorizzati all'interno del documento {@link Parcheggio}.
 */
public class ConfigurazionePiano {

    /**
     * Numero identificativo del piano.
     */
    private int piano;
    /**
     * Numero di posti configurati per il piano.
     */
    private int numeroPosti;
    /** Elenco dei posti embedded nel piano. */
    private List<Posto> posti = new ArrayList<>();

    /**
     * Costruttore vuoto richiesto dal framework di persistenza.
     */
    public ConfigurazionePiano() {
    }

    /**
     * Crea una nuova istanza di ConfigurazionePiano con i dati indicati.
     *
     * @param piano piano da filtrare o modificare
     * @param numeroPosti capacità configurata del piano
     * @param posti elenco dei posti del piano
     */
    public ConfigurazionePiano(int piano, int numeroPosti, List<Posto> posti) {
        this.piano = piano;
        this.numeroPosti = numeroPosti;
        this.posti = posti == null ? new ArrayList<>() : new ArrayList<>(posti);
    }

    /**
     * Restituisce numero del piano.
     * @return numero del piano
     */
    public int getPiano() {
        return piano;
    }

    /**
     * Imposta numero del piano.
     *
     * @param piano piano da filtrare o modificare
     */
    public void setPiano(int piano) {
        this.piano = piano;
    }

    /**
     * Restituisce numero di posti.
     * @return capacità configurata
     */
    public int getNumeroPosti() {
        return numeroPosti;
    }

    /**
     * Imposta numero di posti.
     *
     * @param numeroPosti capacità configurata del piano
     */
    public void setNumeroPosti(int numeroPosti) {
        this.numeroPosti = numeroPosti;
    }

    /**
     * Recupera i posti di un parcheggio, eventualmente filtrati per piano.
     * @return elenco dei posti
     */
    public List<Posto> getPosti() {
        if (posti == null) {
            posti = new ArrayList<>();
        }
        return posti;
    }

    /**
     * Imposta elenco dei posti.
     *
     * @param posti elenco dei posti del piano
     */
    public void setPosti(List<Posto> posti) {
        this.posti = posti == null ? new ArrayList<>() : new ArrayList<>(posti);
    }
}
