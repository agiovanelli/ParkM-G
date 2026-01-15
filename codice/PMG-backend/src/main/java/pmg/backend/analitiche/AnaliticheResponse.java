package pmg.backend.analitiche;

import java.util.List;

import pmg.backend.log.LogResponse;

public class AnaliticheResponse {

    private String id;
    private String parcheggioId;
    private String nomeParcheggio;
    private String operatoreId;
    private List<LogResponse> log;

    public AnaliticheResponse() {}

    public AnaliticheResponse(String id, String parcheggioId, String nomeParcheggio,
                              String operatoreId, List<LogResponse> logEventi) {
        this.id = id;
        this.parcheggioId = parcheggioId;
        this.nomeParcheggio = nomeParcheggio;
        this.operatoreId = operatoreId;
        this.log = logEventi;
    }

    public String getId() {
        return id;
    }

    public String getParcheggioId() {
        return parcheggioId;
    }

    public String getNomeParcheggio() {
        return nomeParcheggio;
    }

    public String getOperatoreId() {
        return operatoreId;
    }

    public List<LogResponse> getLog() {
        return log;
    }
}
