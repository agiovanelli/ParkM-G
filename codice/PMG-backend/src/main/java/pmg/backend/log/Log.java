package pmg.backend.log;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "log")
public class Log {

    @Id
    private String id;

    private String analiticaId;
    private LogCategoria tipo; 
    private LogSeverità severità;
    private String titolo;
    private String descrizione;
    private LocalDateTime data;
    
    public String getId() {
        return id;
    }

    public String getAnaliticaId() {
        return analiticaId;
    }

    public LogCategoria getTipo() {
        return tipo;
    }
    
    public LogSeverità getSeverità() {
        return severità;
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
    
    public void setTipo(LogCategoria tipo) {
        this.tipo = tipo;
    }
    
    public void setSeverità(LogSeverità severità) {
        this.severità = severità;
    }
    
    public void getTitolo(String titolo) {
        this.titolo = titolo;
    }
    
    public void getDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
	public void setData(LocalDateTime data) {
		this.data = data;
	}
}
