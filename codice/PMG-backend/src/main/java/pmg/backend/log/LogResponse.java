package pmg.backend.log;

import java.time.LocalDateTime;

public class LogResponse {
	
    private String id;
    private String tipo;
    private String descrizione;
    private LocalDateTime data;

    public LogResponse() {}

    public LogResponse(String id, String tipo, String descrizione, LocalDateTime data) {
        this.id = id;
        this.tipo = tipo;
        this.descrizione = descrizione;
        this.data = data;
    }
    
    public String getId() {
        return id;
    }

    public String getTipoId() {
        return tipo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public LocalDateTime getData() {
        return data;
    }
}
