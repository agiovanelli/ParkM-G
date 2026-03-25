package pmg.backend.posto;

public class PostoResponse {

	private String id;
    private int piano;
    private int distanzaUscita;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;
    private String parcheggioId;
    private boolean disponibile;

    public PostoResponse(Posto p) {
    	this.id = p.getId();
        this.piano = p.getPiano();
        this.distanzaUscita = p.getDistanzaUscita();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.parcheggioId = p.getParcheggioId();
        this.disponibile = p.isDisponibile();
    }

    public String getId() { return id; }
    public int getDistanzaUscita() { return distanzaUscita; }
    public boolean getRiservatoDisabili() { return riservatoDisabili; }
    public boolean getRiservanoIncinta() { return riservatoIncinta; }
    public int getPiano() { return piano; }
    public String getParcheggioId() { return parcheggioId; }
    public boolean isDisponibile() { return disponibile; }
}