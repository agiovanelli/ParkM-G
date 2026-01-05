package pmg.backend.log;

import java.time.LocalDateTime;

public class LogResponse {
	
    private String id;
    private String tipo;
    private String titolo;
    private String descrizione;
    private LocalDateTime data;
    private String severita;

    public LogResponse() {}

    public LogResponse(String id, String tipo, String titolo, String descrizione, LocalDateTime data, String severita) {
        this.id = id;
        this.tipo = tipo;
        this.titolo= titolo;
        this.descrizione = descrizione;
        this.data = data;
        this.severita = severita;
    }
    
    public String getId() {
        return id;
    }

    public String getTipoId() {
        return tipo;
    }

    public String getTitolo() {
        return titolo;
    }
    
    public String getDescrizione() {
        return descrizione;
    }

    public LocalDateTime getData() {
        return data;
    }
    
    public String getSeverita() {
        return severita;
    }
}
