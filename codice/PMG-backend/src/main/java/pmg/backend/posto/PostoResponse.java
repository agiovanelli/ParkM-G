package pmg.backend.posto;

/**
 * DTO utilizzato per restituire i dati di un posto auto.
 *
 * Include le informazioni principali del posto,
 * come posizione, disponibilità e caratteristiche.
 */
public class PostoResponse {

    /** Identificativo del posto. */
    private String id;

    /** Identificativo del parcheggio associato. */
    private String parcheggioId;

    /** Numero del posto. */
    private int numero;

    /** Piano in cui si trova il posto. */
    private int piano;

    /** Indica se il posto è disponibile. */
    private boolean disponibile;

    /** Indica se il posto è disabilitato. */
    private boolean disabilitato;

    /** Indica se il posto è riservato a persone con disabilità. */
    private boolean riservatoDisabili;

    /** Indica se il posto è riservato a donne in gravidanza. */
    private boolean riservatoIncinta;

    /** Distanza del posto dall'uscita. */
    private int distanzaUscita;

    /**
     * Costruttore vuoto.
     */
    public PostoResponse() {}

    /**
     * Crea una risposta a partire da un'entità Posto.
     *
     * @param p entità posto da cui estrarre i dati
     */
    public PostoResponse(Posto p) {
        this.id = p.getId();
        this.parcheggioId = p.getParcheggioId();
        this.numero = p.getNumero();
        this.piano = p.getPiano();
        this.disponibile = p.isDisponibile();
        this.disabilitato = p.isDisabilitato();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.distanzaUscita = p.getDistanzaUscita();
    }
    
    /**
     * Restituisce l'identificativo del posto.
     *
     * @return identificativo del posto
     */
    public String getId() { return id; }

    /**
     * Restituisce l'identificativo del parcheggio.
     *
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() { return parcheggioId; }

    /**
     * Restituisce il numero del posto.
     *
     * @return numero del posto
     */
    public int getNumero() { return numero; }

    /**
     * Restituisce il piano del posto.
     *
     * @return piano del posto
     */
    public int getPiano() { return piano; }

    /**
     * Indica se il posto è disponibile.
     *
     * @return true se disponibile, false altrimenti
     */
    public boolean isDisponibile() { return disponibile; }

    /**
     * Indica se il posto è disabilitato.
     *
     * @return true se disabilitato, false altrimenti
     */
    public boolean isDisabilitato() { return disabilitato; }

    /**
     * Indica se il posto è riservato a persone con disabilità.
     *
     * @return true se riservato, false altrimenti
     */
    public boolean isRiservatoDisabili() { return riservatoDisabili; }

    /**
     * Indica se il posto è riservato a donne in gravidanza.
     *
     * @return true se riservato, false altrimenti
     */
    public boolean isRiservatoIncinta() { return riservatoIncinta; }

    /**
     * Restituisce la distanza dall'uscita.
     *
     * @return distanza dall'uscita
     */
    public int getDistanzaUscita() { return distanzaUscita; }

    /**
     * Imposta la disponibilità del posto.
     *
     * @param disponibile nuovo stato di disponibilità
     */
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
}