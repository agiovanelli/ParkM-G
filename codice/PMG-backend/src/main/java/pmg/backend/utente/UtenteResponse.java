package pmg.backend.utente;

import java.util.Map;

/**
 * DTO utilizzato per restituire i dati di un utente.
 *
 * Include le informazioni principali dell'utente
 * e le preferenze associate.
 */
public class UtenteResponse {

    /** Identificativo dell'utente. */
    private String id;

    /** Nome dell'utente. */
    private String nome;

    /** Cognome dell'utente. */
    private String cognome;

    /** Email dell'utente. */
    private String email;

    /** Username dell'utente. */
    private String username;

    /** Preferenze dell'utente espresse come mappa chiave-valore. */
    private Map<String, String> preferenze;

    /**
     * Costruttore vuoto.
     */
    public UtenteResponse() {
    }

    /**
     * Crea una risposta contenente i dati dell'utente.
     *
     * @param id identificativo dell'utente
     * @param nome nome dell'utente
     * @param cognome cognome dell'utente
     * @param email email dell'utente
     * @param username username dell'utente
     * @param preferenze mappa delle preferenze
     */
    public UtenteResponse(String id, String nome, String cognome, String email, String username, Map<String, String> preferenze) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.username = username;
        this.preferenze = preferenze;
    }

    /**
     * Restituisce l'identificativo dell'utente.
     *
     * @return identificativo dell'utente
     */
    public String getId() {
        return id;
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
}