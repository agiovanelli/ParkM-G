package pmg.backend.operatore;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB degli operatori.
 *
 * Contiene le informazioni principali associate a un operatore
 * e al parcheggio gestito.
 */
@Document(collection = "operatori")
public class Operatore {

    /** Identificativo univoco del documento. */
    @Id
    private String id;

    /** Identificativo del parcheggio associato. */
    private String parcheggioId;

    /** Nome della struttura associata al parcheggio. */
    private String nomeStruttura;

    /** Username dell'operatore. */
    private String username;

    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Operatore() {
    }

    // GETTER / SETTER

    /**
     * Restituisce l'identificativo del documento.
     *
     * @return identificativo del documento
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta l'identificativo del documento.
     *
     * @param id nuovo identificativo del documento
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Restituisce il nome della struttura associata.
     *
     * @return nome della struttura
     */
    public String getNomeStruttura() {
        return nomeStruttura;
    }

    /**
     * Imposta il nome della struttura associata.
     *
     * @param nomeStruttura nuovo nome della struttura
     */
    public void setNomeStruttura(String nomeStruttura) {
        this.nomeStruttura = nomeStruttura;
    }

    /**
     * Restituisce lo username dell'operatore.
     *
     * @return username dell'operatore
     */
    public String getUsername() {
        return username;
    }

    /**
     * Imposta lo username dell'operatore.
     *
     * @param username nuovo username dell'operatore
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Restituisce l'identificativo del parcheggio associato.
     *
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() { 
        return parcheggioId; 
    }

    /**
     * Imposta l'identificativo del parcheggio associato.
     *
     * @param parcheggioId nuovo identificativo del parcheggio
     */
    public void setParcheggioId(String parcheggioId) { 
        this.parcheggioId = parcheggioId; 
    }
}