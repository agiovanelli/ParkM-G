package pmg.backend.operatore;

public class OperatoreResponse {
    private String id;
    private String username;
    private String nomeStruttura;
    private String parcheggioId;

    // Costruttore vuoto (Spring Data)
    public OperatoreResponse() {
    }

    // Costruttore
    public OperatoreResponse(String id, String username, String nomeStruttura, String parcheggioId) {
        this.id = id;
        this.username = username;
        this.nomeStruttura = nomeStruttura;
        this.parcheggioId = parcheggioId;
    }

    // Getter
    public String getId() { return id; }
    public String getParcheggioId() { return parcheggioId; }
    public String getUsername() { return username; }
    public String getNomeStruttura() { return nomeStruttura; }
}