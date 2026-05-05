package pmg.backend.posto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB dei posti auto.
 *
 * Contiene le informazioni principali associate a un singolo posto
 * all'interno di un parcheggio, incluse disponibilità e caratteristiche.
 */
@Document(collection = "posti")
public class Posto {

    /** Identificativo univoco del documento. */
	@Id
	private String id;

    /** Identificativo del parcheggio associato. */
	private String parcheggioId;

    /** Numero identificativo del posto. */
	private int numero;

    /** Piano in cui si trova il posto. */
	private int piano;

    /** Indica se il posto è disponibile. */
    private boolean disponibile;

    /** Indica se il posto è disabilitato (non utilizzabile). */
    private boolean disabilitato;

    /** Indica se il posto è riservato a persone con disabilità. */
    private boolean riservatoDisabili;

    /** Indica se il posto è riservato a donne in gravidanza. */
    private boolean riservatoIncinta;

    /** Distanza del posto dall'uscita. */
    private int distanzaUscita;

    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Posto() {}

    /**
     * Crea un nuovo posto auto con le informazioni principali.
     *
     * @param id identificativo del posto
     * @param parcheggioId identificativo del parcheggio
     * @param numero numero del posto
     * @param piano piano del posto
     * @param postoBoolean oggetto contenente gli stati booleani del posto
     * @param distanzaUscita distanza dall'uscita
     */
    public Posto(String id, String parcheggioId, int numero, int piano, PostoBoolean postoBoolean, int distanzaUscita) {
    	this.id = id;
    	this.parcheggioId = parcheggioId;
    	this.numero = numero;
    	this.piano = piano;
        this.disponibile = postoBoolean.disponibile();
        this.disabilitato = postoBoolean.disabilitato();
        this.riservatoDisabili = postoBoolean.riservatoDisabili();
        this.riservatoIncinta = postoBoolean.riservatoIncinta();
        this.distanzaUscita = distanzaUscita;
    }

    // Getter & Setter

    /**
     * Indica se il posto è disponibile.
     *
     * @return true se disponibile, false altrimenti
     */
    public boolean isDisponibile() { return disponibile; }

    /**
     * Imposta la disponibilità del posto.
     *
     * @param disponibile nuovo stato di disponibilità
     */
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
    
    /**
     * Indica se il posto è disabilitato.
     *
     * @return true se disabilitato, false altrimenti
     */
    public boolean isDisabilitato() { return disabilitato; }

    /**
     * Imposta lo stato di disabilitazione del posto.
     *
     * @param disabilitato nuovo stato di disabilitazione
     */
    public void setDisabilitato(boolean disabilitato) { this.disabilitato = disabilitato; }

    /**
     * Indica se il posto è riservato a persone con disabilità.
     *
     * @return true se riservato, false altrimenti
     */
    public boolean isRiservatoDisabili() { return riservatoDisabili; }

    /**
     * Imposta lo stato di riserva per disabili.
     *
     * @param riservatoDisabili nuovo stato
     */
    public void setRiservatoDisabili(boolean riservatoDisabili) { this.riservatoDisabili = riservatoDisabili; }

    /**
     * Indica se il posto è riservato a donne in gravidanza.
     *
     * @return true se riservato, false altrimenti
     */
    public boolean isRiservatoIncinta() { return riservatoIncinta; }

    /**
     * Imposta lo stato di riserva per donne in gravidanza.
     *
     * @param riservatoIncinta nuovo stato
     */
    public void setRiservatoIncinta(boolean riservatoIncinta) { this.riservatoIncinta = riservatoIncinta; }

    /**
     * Restituisce la distanza dall'uscita.
     *
     * @return distanza dall'uscita
     */
    public int getDistanzaUscita() { return distanzaUscita; }

    /**
     * Imposta la distanza dall'uscita.
     *
     * @param distanzaUscita nuova distanza
     */
    public void setDistanzaUscita(int distanzaUscita) { this.distanzaUscita = distanzaUscita; }
    
    /**
     * Restituisce l'identificativo del parcheggio associato.
     *
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() { return parcheggioId; }

    /**
     * Imposta l'identificativo del parcheggio associato.
     *
     * @param parcheggioId nuovo identificativo del parcheggio
     */
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }
    
    /**
     * Restituisce il numero del posto.
     *
     * @return numero del posto
     */
    public int getNumero() { return numero; }

    /**
     * Imposta il numero del posto.
     *
     * @param numero nuovo numero del posto
     */
    public void setNumero(int numero) { this.numero = numero; }
    
    /**
     * Restituisce il piano del posto.
     *
     * @return piano del posto
     */
    public int getPiano() { return piano; }

    /**
     * Imposta il piano del posto.
     *
     * @param piano nuovo piano del posto
     */
    public void setPiano(int piano) { this.piano = piano; }
    
    /**
     * Restituisce l'identificativo del documento.
     *
     * @return identificativo del documento
     */
    public String getId() { return id; }

    /**
     * Imposta l'identificativo del documento.
     *
     * @param id nuovo identificativo del documento
     */
    public void setId(String id) { this.id = id; }
}

/**
 * Rappresenta un contenitore per gli stati booleani di un posto auto.
 *
 * Permette di raggruppare le caratteristiche principali del posto
 * in un'unica struttura immutabile.
 *
 * @param disponibile indica se il posto è disponibile
 * @param disabilitato indica se il posto è disabilitato
 * @param riservatoDisabili indica se il posto è riservato a disabili
 * @param riservatoIncinta indica se il posto è riservato a donne in gravidanza
 */
record PostoBoolean(boolean disponibile, boolean disabilitato, boolean riservatoDisabili, boolean riservatoIncinta) {}