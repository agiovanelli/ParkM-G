package pmg.backend.posto;

public class PostoResponse {

    private String id;
    private String parcheggioId;
    private boolean disponibile;
    private int piano;

    public PostoResponse(String id, String parcheggioId,
                         boolean disponibile, int piano) {
        this.id = id;
        this.parcheggioId = parcheggioId;
        this.disponibile = disponibile;
        this.piano = piano;
    }

    public String getId() { return id; }
    public String getParcheggioId() { return parcheggioId; }
    public boolean isDisponibile() { return disponibile; }
    public int getPiano() { return piano; }
}