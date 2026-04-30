package pmg.backend.analitiche;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB delle analitiche.
 *
 * Contiene le informazioni principali associate a un parcheggio
 * e all'operatore collegato.
 */
@Document(collection = "analitiche")
public class Analitiche {

	/** Identificativo univoco del documento. */
    @Id
    private String id;

    /** Identificativo del parcheggio associato. */
    private String parcheggioId;
    
    /** Nome del parcheggio associato. */
    private String nomeParcheggio;
    
    /** Identificativo dell'operatore associato. */
    private String operatoreId;

    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Analitiche() {}

    /**
     * Crea una nuova analitica associata a un parcheggio e a un operatore.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param nomeParcheggio nome del parcheggio
     * @param operatoreId identificativo dell'operatore
     */
    public Analitiche(
            String parcheggioId,
            String nomeParcheggio,
            String operatoreId) {

        this.parcheggioId = parcheggioId;
        this.nomeParcheggio = nomeParcheggio;
        this.operatoreId = operatoreId;
    }

    /**
     * Restituisce l'identificativo del documento.
     *
     * @return identificativo del documento
     */
    public String getId() {
        return id;
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
     * Restituisce il nome del parcheggio associato.
     *
     * @return nome del parcheggio
     */
    public String getNomeParcheggio() {
        return nomeParcheggio;
    }

    /**
     * Restituisce l'identificativo dell'operatore associato.
     *
     * @return identificativo dell'operatore
     */
    public String getOperatoreId() {
        return operatoreId;
    }
    
    /**
     * Imposta l'identificativo del documento.
     *
     * @param id nuovo identificativo del documento
     */
    public void setId(String id) {
    	this.id = id;
    }
}
