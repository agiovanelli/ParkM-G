package pmg.backend.log;

import java.time.LocalDateTime;

/**
 * DTO utilizzato per restituire i dati di un log.
 *
 * Contiene le informazioni principali del log da inviare
 * come risposta al client.
 */
public class LogResponse {
	
    /** Identificativo del log. */
    private String id;
    
    /** Categoria del log. */
    private String tipo;
    
    /** Titolo del log. */
    private String titolo;
    
    /** Descrizione del log. */
    private String descrizione;
    
    /** Data e ora del log. */
    private LocalDateTime data;
    
    /** Livello di severità del log. */
    private String severita;

    /**
     * Costruttore vuoto.
     */
    public LogResponse() {}

    /**
     * Crea una risposta contenente i dati del log.
     *
     * @param id identificativo del log
     * @param analiticaId identificativo dell'analitica associata
     * @param tipo categoria del log
     * @param titolo titolo del log
     * @param descrizione descrizione del log
     * @param data data e ora del log
     * @param severita livello di severità del log
     */
    public LogResponse(String id, String analiticaId, String tipo, String titolo, String descrizione, LocalDateTime data, String severita) {
        this.id = id;
        this.tipo = tipo;
        this.titolo= titolo;
        this.descrizione = descrizione;
        this.data = data;
        this.severita = severita;
    }
    
    /**
     * Restituisce l'identificativo del log.
     *
     * @return identificativo del log
     */
    public String getId() {
        return id;
    }

    /**
     * Restituisce la categoria del log.
     *
     * @return categoria del log
     */
    public String getTipo() {
        return tipo;
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
     * Restituisce il livello di severità del log.
     *
     * @return livello di severità del log
     */
    public String getSeverita() {
        return severita;
    }
}