package pmg.backend.operatore;

/**
 * DTO utilizzato per restituire i dati di un operatore.
 *
 * Include le informazioni principali dell'operatore
 * e del parcheggio associato.
 */
public class OperatoreResponse {

    /** Identificativo dell'operatore. */
    private String id;

    /** Username dell'operatore. */
    private String username;

    /** Nome della struttura associata. */
    private String nomeStruttura;

    /** Identificativo del parcheggio associato. */
    private String parcheggioId;

    /**
     * Costruttore vuoto.
     */
    public OperatoreResponse() {
    }

    /**
     * Crea una risposta contenente i dati dell'operatore.
     *
     * @param id identificativo dell'operatore
     * @param username username dell'operatore
     * @param nomeStruttura nome della struttura
     * @param parcheggioId identificativo del parcheggio
     */
    public OperatoreResponse(String id, String username, String nomeStruttura, String parcheggioId) {
        this.id = id;
        this.username = username;
        this.nomeStruttura = nomeStruttura;
        this.parcheggioId = parcheggioId;
    }

    /**
     * Restituisce l'identificativo dell'operatore.
     *
     * @return identificativo dell'operatore
     */
    public String getId() { return id; }

    /**
     * Restituisce l'identificativo del parcheggio associato.
     *
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() { return parcheggioId; }

    /**
     * Restituisce lo username dell'operatore.
     *
     * @return username dell'operatore
     */
    public String getUsername() { return username; }

    /**
     * Restituisce il nome della struttura associata.
     *
     * @return nome della struttura
     */
    public String getNomeStruttura() { return nomeStruttura; }
}