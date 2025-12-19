package pmg.backend.utente;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "utenti")
public class Utente {

    @Id
    private String id; //ObjectId

    private String nome;
    private String cognome;
    private String email;
    private String username;
    private String password;

    // Preferenze come mappa (equivalente al tuo "preferenze" annidato)
    private Map<String, String> preferenze;

    public Utente() {
    }

    public Utente(String nome, String cognome, String email, String username, String password) {
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    // GETTER / SETTER

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getPreferenze() {
        return preferenze;
    }

    public void setPreferenze(Map<String, String> preferenze) {
        this.preferenze = preferenze;
    }
}
