package pmg.backend.operatore;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "operatori")
public class Operatore {

    @Id
    private String id; //ObjectId
    private String parcheggioId;
    private String nomeStruttura;
    private String username;

    // Costruttore vuoto richiesto da Spring Data
    public Operatore() {
    }

    public Operatore(String nomeStruttura, String username, String parcheggioId ) {
        this.nomeStruttura = nomeStruttura;
        this.username = username;
        this.parcheggioId = parcheggioId;
    }

    // GETTER / SETTER

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomeStruttura() {
        return nomeStruttura;
    }

    public void setNomeStruttura(String nomeStruttura) {
        this.nomeStruttura = nomeStruttura;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getParcheggioId() { return parcheggioId; }
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }
}
