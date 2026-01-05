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
    private LogSeverità severita;
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
    
    public LogSeverità getSeverita() {
        return severita;
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
    
    public void setSeverita(LogSeverità severita) {
        this.severita = severita;
    }
    
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
	public void setData(LocalDateTime data) {
		this.data = data;
	}
}
