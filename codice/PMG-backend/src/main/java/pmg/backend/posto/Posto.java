package pmg.backend.posto;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "posti")
public class Posto {

    private boolean disponibile;
    private boolean disabilitato;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;

    private int distanzaUscita;

    public Posto() {}

    public Posto(boolean disponibile, boolean disabilitato,
                 boolean riservatoDisabili,
                 boolean riservatoIncinta,
                 int distanzaUscita) {
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
}