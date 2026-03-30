package pmg.backend.posto;

public class PostoResponse {

    private String slotId;
    private int floor;
    private String slotNumber;
    private int distanzaUscita;
    private boolean disabilitato;
    private boolean riservatoDisabili;
    private boolean riservatoIncinta;
    private boolean disponibile;
    
    public PostoResponse() {
    }

    // Costruttore semplice: serve a PostoServiceImpl
    public PostoResponse(Posto p) {
        this.slotId = null;
        this.floor = 0;
        this.slotNumber = null;
        this.distanzaUscita = p.getDistanzaUscita();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.disponibile = p.isDisponibile();
        this.disabilitato = p.isDisabilitato();
    }

    // Costruttore completo: serve a ParcheggioServiceImpl
    public PostoResponse(
            String slotId,
            int floor,
            String slotNumber,
            Posto p
    ) {
        this.slotId = slotId;
        this.floor = floor;
        this.slotNumber = slotNumber;
        this.distanzaUscita = p.getDistanzaUscita();
        this.riservatoDisabili = p.isRiservatoDisabili();
        this.riservatoIncinta = p.isRiservatoIncinta();
        this.disponibile = p.isDisponibile();
        this.disabilitato = p.isDisabilitato();
    }

    public String getSlotId() {
        return slotId;
    }

    public int getFloor() {
        return floor;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public int getDistanzaUscita() {
        return distanzaUscita;
    }

    public boolean getRiservatoDisabili() {
        return riservatoDisabili;
    }

    public boolean getRiservatoIncinta() {
        return riservatoIncinta;
    }

    public boolean isDisponibile() {
        return disponibile;
    }
    
    public boolean isDisabilitato() {
        return disabilitato;
    }
    
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public void setFloor(int floor) { this.floor = floor; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }
    public void setDistanzaUscita(int distanzaUscita) { this.distanzaUscita = distanzaUscita; }
    public void setRiservatoDisabili(boolean riservatoDisabili) { this.riservatoDisabili = riservatoDisabili; }
    public void setRiservatoIncinta(boolean riservatoIncinta) { this.riservatoIncinta = riservatoIncinta; }
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
    public void setDisabilitato(boolean disabilitato) { this.disabilitato = disabilitato; }
}