package pmg.backend.parcheggio;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB dei parcheggi.
 *
 * Contiene le informazioni principali associate a un parcheggio,
 * come posizione, capacità e stato di disponibilità.
 */
@Document(collection = "parcheggi")
public class Parcheggio {

    /** Identificativo univoco del documento. */
    @Id
    private String id;

    /** Nome del parcheggio. */
    private String nome;

    /** Area geografica in cui si trova il parcheggio. */
    private String area;

    /** Numero totale di posti disponibili nel parcheggio. */
    private int postiTotali;

    /** Numero di posti attualmente disponibili. */
    private int postiDisponibili;

    /** Latitudine della posizione del parcheggio. */
    private double latitudine;

    /** Longitudine della posizione del parcheggio. */
    private double longitudine;

    /** Indica se il parcheggio è in stato di emergenza. */
    private boolean inEmergenza;

    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Parcheggio() {}

    /**
     * Crea un nuovo parcheggio con le informazioni principali.
     *
     * @param nome nome del parcheggio
     * @param area area geografica del parcheggio
     * @param postiTotali numero totale di posti
     * @param postiDisponibili numero di posti disponibili
     * @param latitudine latitudine della posizione
     * @param longitudine longitudine della posizione
     */
    public Parcheggio(String nome, String area, int postiTotali, int postiDisponibili,
                      double latitudine, double longitudine) {
        this.nome = nome;
        this.area = area;
        this.postiTotali = postiTotali;
        this.postiDisponibili = postiDisponibili;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    // Getter e Setter 

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

    /**
     * Restituisce il nome del parcheggio.
     *
     * @return nome del parcheggio
     */
    public String getNome() { return nome; }

    /**
     * Restituisce l'area geografica del parcheggio.
     *
     * @return area del parcheggio
     */
    public String getArea() { return area; }

    /**
     * Restituisce il numero totale di posti.
     *
     * @return numero totale di posti
     */
    public int getPostiTotali() { return postiTotali; }

    /**
     * Imposta il numero totale di posti.
     *
     * @param postiTotali nuovo numero totale di posti
     */
    public void setPostiTotali(int postiTotali) { this.postiTotali = postiTotali; }

    /**
     * Restituisce il numero di posti disponibili.
     *
     * @return numero di posti disponibili
     */
    public int getPostiDisponibili() { return postiDisponibili; }

    /**
     * Imposta il numero di posti disponibili.
     *
     * @param postiDisponibili nuovo numero di posti disponibili
     */
    public void setPostiDisponibili(int postiDisponibili) { this.postiDisponibili = postiDisponibili; }

    /**
     * Restituisce la latitudine del parcheggio.
     *
     * @return latitudine
     */
    public double getLatitudine() { return latitudine; }

    /**
     * Restituisce la longitudine del parcheggio.
     *
     * @return longitudine
     */
    public double getLongitudine() { return longitudine; }

    /**
     * Indica se il parcheggio è in stato di emergenza.
     *
     * @return true se in emergenza, false altrimenti
     */
    public boolean isInEmergenza() { return inEmergenza; }

    /**
     * Imposta lo stato di emergenza del parcheggio.
     *
     * @param inEmergenza nuovo stato di emergenza
     */
    public void setInEmergenza(boolean inEmergenza) { this.inEmergenza = inEmergenza; }
}