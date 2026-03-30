package pmg.backend.posto;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "posti")
public class Posto {

	@Id
	private String id;
	private String parcheggioId;
	private int numero;
	private int piano;
    private boolean disponibile;
    private boolean disabilitato;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;
    private int distanzaUscita;

    public Posto() {}

    public Posto(String id, String parcheggioId, int numero, int piano, boolean disponibile, boolean disabilitato,
                 boolean riservatoDisabili,
                 boolean riservatoIncinta,
                 int distanzaUscita) {
    	this.id = id;
    	this.parcheggioId = parcheggioId;
    	this.numero = numero;
    	this.piano = piano;
        this.disponibile = disponibile;
        this.disabilitato = disabilitato;
        this.riservatoDisabili = riservatoDisabili;
        this.riservatoIncinta = riservatoIncinta;
        this.distanzaUscita = distanzaUscita;
    }

    // Getter & Setter
    public boolean isDisponibile() { return disponibile; }
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
    
    public boolean isDisabilitato() { return disabilitato; }
    public void setDisabilitato(boolean disabilitato) { this.disabilitato = disabilitato; }

    public boolean isRiservatoDisabili() { return riservatoDisabili; }
    public void setRiservatoDisabili(boolean riservatoDisabili) { this.riservatoDisabili = riservatoDisabili; }

    public boolean isRiservatoIncinta() { return riservatoIncinta; }
    public void setRiservatoIncinta(boolean riservatoIncinta) { this.riservatoIncinta = riservatoIncinta; }

    public int getDistanzaUscita() { return distanzaUscita; }
    public void setDistanzaUscita(int distanzaUscita) { this.distanzaUscita = distanzaUscita; }
    
    public String getParcheggioId() { return parcheggioId; }
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }
    
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    
    public int getPiano() { return piano; }
    public void setPiano(int piano) { this.piano = piano; }
    
    public String getId() { return id; }
}