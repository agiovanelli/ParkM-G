package pmg.backend.log;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "log")
public class Log {

    @Id
    private String id;

    private String analiticaId;
    private String tipo;          // Evento | Allarme
    private String descrizione;
    private LocalDateTime data;
    
    public String getId() {
        return id;
    }

    public String getAnaliticaId() {
        return analiticaId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescrizione() {
        return descrizione;
    }
    
    public LocalDateTime getData() {
        return data;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public void getDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
	public void setData(LocalDateTime data) {
		this.data = data;
	}
}
