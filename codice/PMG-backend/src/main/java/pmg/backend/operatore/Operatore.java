package pmg.backend.operatore;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "operatori")
public class Operatore {

    @Id
    private String id;
    private String parcheggioId;
    private String nomeStruttura;
    private String username;

    // Costruttore vuoto (Spring Data)
    public Operatore() {
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
    
    public String getParcheggioId() { 
    	return parcheggioId; 
    }
    
    public void setParcheggioId(String parcheggioId) { 
    	this.parcheggioId = parcheggioId; 
    }
}
