package pmg.backend.operatore;

public class OperatoreLoginRequest {
    private String nomeStruttura;
    private String username;

    public OperatoreLoginRequest() {}

    public OperatoreLoginRequest(String nomeStruttura, String username) {
        this.nomeStruttura = nomeStruttura;
        this.username = username;
    }

    public String getNomeStruttura() { return nomeStruttura; }
    public void setNomeStruttura(String nomeStruttura) { this.nomeStruttura = nomeStruttura; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}