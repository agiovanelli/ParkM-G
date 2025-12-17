package pmg.backend.operatore;

public class OperatoreResponse {
    private String id;
    private String username;
    private String nomeStruttura;

    // 1. Costruttore VUOTO (necessario per Spring/Jackson)
    public OperatoreResponse() {
    }

    // 2. Costruttore con 3 PARAMETRI (quello che ti manca!)
    public OperatoreResponse(String id, String username, String nomeStruttura) {
        this.id = id;
        this.username = username;
        this.nomeStruttura = nomeStruttura;
    }

    // 3. Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNomeStruttura() { return nomeStruttura; }
    public void setNomeStruttura(String nomeStruttura) { this.nomeStruttura = nomeStruttura; }
}