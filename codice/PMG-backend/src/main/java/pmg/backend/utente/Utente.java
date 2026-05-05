package pmg.backend.utente;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Rappresenta un documento della collezione MongoDB degli utenti.
 *
 * Contiene le informazioni principali associate a un utente,
 * incluse credenziali e preferenze.
 */
@Document(collection = "utenti")
public class Utente {

    /** Identificativo univoco del documento. */
    @Id
    private String id; //ObjectId

    /** Nome dell'utente. */
    private String nome;

    /** Cognome dell'utente. */
    private String cognome;

    /** Email dell'utente. */
    private String email;

    /** Username dell'utente. */
    private String username;

    /** Password dell'utente. */
    private String password;

    /** Preferenze dell'utente espresse come mappa chiave-valore. */
    private Map<String, String> preferenze;

    /**
     * Costruttore vuoto richiesto da Spring Data MongoDB.
     */
    public Utente() {
    }

    /**
     * Crea un nuovo utente con le informazioni principali.
     *
     * @param nome nome dell'utente
     * @param cognome cognome dell'utente
     * @param email email dell'utente
     * @param username username dell'utente
     * @param password password dell'utente
     */
    public Utente(String nome, String cognome, String email, String username, String password) {
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    // GETTER / SETTER

    /**
     * Restituisce l'identificativo del documento.
     *
     * @return identificativo del documento
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta l'identificativo del documento.
     *
     * @param id nuovo identificativo del documento
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Restituisce il nome dell'utente.
     *
     * @return nome dell'utente
     */
    public String getNome() {
        return nome;
    }

    /**
     * Restituisce il cognome dell'utente.
     *
     * @return cognome dell'utente
     */
    public String getCognome() {
        return cognome;
    }

    /**
     * Restituisce l'email dell'utente.
     *
     * @return email dell'utente
     */
    public String getEmail() {
        return email;
    }

    /**
     * Restituisce lo username dell'utente.
     *
     * @return username dell'utente
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce le preferenze dell'utente.
     *
     * @return mappa delle preferenze
     */
    public Map<String, String> getPreferenze() {
        return preferenze;
    }

    /**
     * Imposta le preferenze dell'utente.
     *
     * @param preferenze nuova mappa delle preferenze
     */
    public void setPreferenze(Map<String, String> preferenze) {
        this.preferenze = preferenze;
    }
}