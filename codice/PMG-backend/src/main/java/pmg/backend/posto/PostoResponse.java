package pmg.backend.posto;

public class PostoResponse {
    private int distanzaUscita;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;
    private boolean disponibile;

    public PostoResponse(Posto p) {
        this.distanzaUscita = p.getDistanzaUscita();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.disponibile = p.isDisponibile();
    }

    public int getDistanzaUscita() { return distanzaUscita; }
    public boolean getRiservatoDisabili() { return riservatoDisabili; }
    public boolean getRiservanoIncinta() { return riservatoIncinta; }
    public boolean isDisponibile() { return disponibile; }
}