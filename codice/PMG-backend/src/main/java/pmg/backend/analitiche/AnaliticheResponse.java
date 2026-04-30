package pmg.backend.analitiche;

import java.util.List;

import pmg.backend.log.LogResponse;

/**
 * DTO utilizzato per restituire i dati di un'analitica.
 *
 * Include le informazioni principali dell'analitica e
 * l'eventuale lista dei log associati.
 */
public class AnaliticheResponse {

    /** Identificativo dell'analitica. */
    private String id;
    
    /** Identificativo del parcheggio associato. */
    private String parcheggioId;
    
    /** Nome del parcheggio associato. */
    private String nomeParcheggio;
    
    /** Identificativo dell'operatore associato. */
    private String operatoreId;
    
    /** Lista dei log associati all'analitica. */
    private List<LogResponse> log;

    /**
     * Costruttore vuoto.
     */
    public AnaliticheResponse() {}

    /**
     * Crea una risposta contenente i dati dell'analitica.
     *
     * @param id identificativo dell'analitica
     * @param parcheggioId identificativo del parcheggio
     * @param nomeParcheggio nome del parcheggio
     * @param operatoreId identificativo dell'operatore
     * @param logEventi lista dei log associati
     */
    public AnaliticheResponse(String id, String parcheggioId, String nomeParcheggio,
                              String operatoreId, List<LogResponse> logEventi) {
        this.id = id;
        this.parcheggioId = parcheggioId;
        this.nomeParcheggio = nomeParcheggio;
        this.operatoreId = operatoreId;
        this.log = logEventi;
    }

    /**
     * Restituisce l'identificativo dell'analitica.
     *
     * @return identificativo dell'analitica
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
     * Restituisce la lista dei log associati all'analitica.
     *
     * @return lista dei log associati
     */
    public List<LogResponse> getLog() {
        return log;
    }
}