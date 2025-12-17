package pmg.backend.utente;

import java.util.Map;

public class UtenteResponse {
    private String id;
    private String nome;
    private String cognome;
    private String email;
    private String username;
    private Map<String, String> preferenze;

    // 1. Costruttore vuoto (indispensabile per Jackson/Spring)
    public UtenteResponse() {
    }

    // 2. Costruttore con parametri (L'ordine DEVE seguire quello del Service)
    public UtenteResponse(String id, String nome, String cognome, String email, String username, Map<String, String> preferenze) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.username = username;
        this.preferenze = preferenze;
    }

    // 3. Metodi Getter (quelli che il test e il service cercano)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getPreferenze() {
        return preferenze;
    }

    public void setPreferenze(Map<String, String> preferenze) {
        this.preferenze = preferenze;
    }
}