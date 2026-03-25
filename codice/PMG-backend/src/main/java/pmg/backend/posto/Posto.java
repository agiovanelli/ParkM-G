package pmg.backend.posto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "posti")
public class Posto {

    @Id
    private String id;

    private String parcheggioId; // riferimento al parcheggio

    private boolean disponibile;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;

    private int piano;
    private int distanzaUscita;

    public Posto() {}

    public Posto(String parcheggioId,
                 boolean disponibile,
                 boolean riservatoDisabili,
                 boolean riservatoIncinta,
                 int piano,
                 int distanzaUscita) {
        this.parcheggioId = parcheggioId;
        this.disponibile = disponibile;
        this.riservatoDisabili = riservatoDisabili;
        this.riservatoIncinta = riservatoIncinta;
        this.piano = piano;
        this.distanzaUscita = distanzaUscita;
    }

    // Getter & Setter

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParcheggioId() { return parcheggioId; }
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }

    public boolean isDisponibile() { return disponibile; }
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }

    public boolean isRiservatoDisabili() { return riservatoDisabili; }
    public void setRiservatoDisabili(boolean riservatoDisabili) { this.riservatoDisabili = riservatoDisabili; }

    public boolean isRiservatoIncinta() { return riservatoIncinta; }
    public void setRiservatoIncinta(boolean riservatoIncinta) { this.riservatoIncinta = riservatoIncinta; }

    public int getPiano() { return piano; }
    public void setPiano(int piano) { this.piano = piano; }

    public int getDistanzaUscita() { return distanzaUscita; }
    public void setDistanzaUscita(int distanzaUscita) { this.distanzaUscita = distanzaUscita; }
}