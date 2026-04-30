package pmg.backend.log;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB dei log.
 *
 * Contiene le informazioni relative a un evento associato
 * a una specifica analitica.
 */
@Document(collection = "log")
public class Log {

    /** Identificativo univoco del log. */
    @Id
    private String id;
    
    /** Identificativo dell'analitica associata. */
    private String analiticaId;
    
    /** Categoria del log. */
    private LogCategoria tipo; 
    
    /** Livello di severità del log. */
    private LogSeverità severita;
    
    /** Titolo sintetico del log. */
    private String titolo;
    
    /** Descrizione dettagliata del log. */
    private String descrizione;
    
    /** Data e ora del log. */
    private LocalDateTime data;
    
    /**
     * Restituisce l'identificativo del log.
     *
     * @return identificativo del log
     */
    public String getId() {
        return id;
    }

    /**
     * Restituisce l'identificativo dell'analitica associata.
     *
     * @return identificativo dell'analitica
     */
    public String getAnaliticaId() {
        return analiticaId;
    }

    /**
     * Restituisce la categoria del log.
     *
     * @return categoria del log
     */
    public LogCategoria getTipo() {
        return tipo;
    }
    
    /**
     * Restituisce il livello di severità del log.
     *
     * @return livello di severità del log
     */
    public LogSeverità getSeverita() {
        return severita;
    }

    /**
     * Restituisce il titolo del log.
     *
     * @return titolo del log
     */
    public String getTitolo() {
        return titolo;
    }
    
    /**
     * Restituisce la descrizione del log.
     *
     * @return descrizione del log
     */
    public String getDescrizione() {
        return descrizione;
    }
    
    /**
     * Restituisce la data e l'ora del log.
     *
     * @return data e ora del log
     */
    public LocalDateTime getData() {
        return data;
    }
    
    /**
     * Imposta l'identificativo dell'analitica associata.
     *
     * @param analiticaId identificativo dell'analitica
     */
    public void setAnaliticaId(String analiticaId) {
        this.analiticaId = analiticaId;
    }
    
    /**
     * Imposta la categoria del log.
     *
     * @param tipo categoria del log
     */
    public void setTipo(LogCategoria tipo) {
        this.tipo = tipo;
    }
    
    /**
     * Imposta il livello di severità del log.
     *
     * @param severita livello di severità del log
     */
    public void setSeverita(LogSeverità severita) {
        this.severita = severita;
    }
    
    /**
     * Imposta il titolo del log.
     *
     * @param titolo titolo del log
     */
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    
    /**
     * Imposta la descrizione del log.
     *
     * @param descrizione descrizione del log
     */
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
	/**
	 * Imposta la data e l'ora del log.
	 *
	 * @param data data e ora del log
	 */
	public void setData(LocalDateTime data) {
		this.data = data;
	}
}