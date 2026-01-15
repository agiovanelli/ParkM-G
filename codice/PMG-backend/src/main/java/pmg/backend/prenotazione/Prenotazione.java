package pmg.backend.prenotazione;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "prenotazioni")
public class Prenotazione {

    @Id
    private String id;
    private String utenteId;
    private String parcheggioId;
    private LocalDateTime orario;
    private String codiceQr; 

    public Prenotazione() {}

    public Prenotazione(String utenteId, String parcheggioId, LocalDateTime orario, String codiceQr) {
        this.utenteId = utenteId;
        this.parcheggioId = parcheggioId;
        this.orario = orario;
        this.codiceQr = codiceQr;
    }

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUtenteId() { return utenteId; }
    public void setUtenteId(String utenteId) { this.utenteId = utenteId; }
    public String getParcheggioId() { return parcheggioId; }
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }
    public LocalDateTime getOrario() { return orario; }
    public void setOrario(LocalDateTime orario) { this.orario = orario; }
    public String getCodiceQr() { return codiceQr; }
    public void setCodiceQr(String codiceQr) { this.codiceQr = codiceQr; }
}