package pmg.backend.posto;

public class PostoResponse {

    private String id;
    private String parcheggioId;
    private int numero;
    private int piano;
    private boolean disponibile;
    private boolean disabilitato;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;
    private int distanzaUscita;

    public PostoResponse() {}

    public PostoResponse(Posto p) {
        this.id = p.getId();
        this.parcheggioId = p.getParcheggioId();
        this.numero = p.getNumero();
        this.piano = p.getPiano();
        this.disponibile = p.isDisponibile();
        this.disabilitato = p.isDisabilitato();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.distanzaUscita = p.getDistanzaUscita();
    }

    public String getId() { return id; }
    public String getParcheggioId() { return parcheggioId; }
    public int getNumero() { return numero; }
    public int getPiano() { return piano; }
    public boolean isDisponibile() { return disponibile; }
    public boolean isDisabilitato() { return disabilitato; }
    public boolean isRiservatoDisabili() { return riservatoDisabili; }
    public boolean isRiservatoIncinta() { return riservatoIncinta; }
    public int getDistanzaUscita() { return distanzaUscita; }

    public void setId(String id) { this.id = id; }
    public void setParcheggioId(String parcheggioId) { this.parcheggioId = parcheggioId; }
    public void setNumero(int numero) { this.numero = numero; }
    public void setPiano(int piano) { this.piano = piano; }
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
    public void setDisabilitato(boolean disabilitato) { this.disabilitato = disabilitato; }
    public void setRiservatoDisabili(boolean riservatoDisabili) { this.riservatoDisabili = riservatoDisabili; }
    public void setRiservatoIncinta(boolean riservatoIncinta) { this.riservatoIncinta = riservatoIncinta; }
    public void setDistanzaUscita(int distanzaUscita) { this.distanzaUscita = distanzaUscita; }
}