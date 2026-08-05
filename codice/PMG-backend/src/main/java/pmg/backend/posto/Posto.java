package pmg.backend.posto;

/**
 * Rappresenta un posto auto embedded nella configurazione di un piano.
 *
 * La classe non è un documento MongoDB autonomo: viene salvata all'interno di
 * {@code configurazionePiani[].posti} del relativo {@link pmg.backend.parcheggio.Parcheggio}.
 */
public class Posto {

    /**
     * Identificativo logico stabile del posto.
     */
    private String slotId;
    /**
     * Numero progressivo del posto nel piano.
     */
    private int numero;
    /**
     * Nome descrittivo.
     */
    private String nome;
    /**
     * Numero identificativo del piano.
     */
    private int piano;
    /**
     * Categoria funzionale del posto.
     */
    private TipoPosto tipo = TipoPosto.NORMALE;
    /**
     * Stato operativo corrente.
     */
    private StatoPosto stato = StatoPosto.LIBERO;
    /**
     * Indica se il posto è temporaneamente inutilizzabile.
     */
    private boolean fuoriServizio;
    /**
     * Indice logico della distanza del posto dall'uscita.
     */
    private int distanzaUscita;

    /**
     * Costruttore vuoto richiesto dal framework di persistenza.
     */
    public Posto() {
    }

    /**
     * Crea una nuova istanza di Posto con i dati indicati.
     *
     * @param slotId identificativo logico del posto
     * @param numero numero progressivo del posto
     * @param nome nome descrittivo
     * @param piano piano da filtrare o modificare
     * @param tipo categoria del posto
     * @param stato nuovo stato operativo
     * @param fuoriServizio nuovo valore della messa fuori servizio
     * @param distanzaUscita distanza dall'uscita
     */
    public Posto(
            String slotId,
            int numero,
            String nome,
            int piano,
            TipoPosto tipo,
            StatoPosto stato,
            boolean fuoriServizio,
            int distanzaUscita) {
        this.slotId = slotId;
        this.numero = numero;
        this.nome = nome;
        this.piano = piano;
        this.tipo = tipo == null ? TipoPosto.NORMALE : tipo;
        this.stato = stato == null ? StatoPosto.LIBERO : stato;
        this.fuoriServizio = fuoriServizio;
        this.distanzaUscita = distanzaUscita;
    }

    /**
     * Estrae l'identificativo logico del posto dalla prenotazione.
     * @return identificativo del posto o {@code null}
     */
    public String getSlotId() {
        return slotId;
    }

    /**
     * Imposta identificativo logico del posto.
     *
     * @param slotId identificativo logico del posto
     */
    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    /**
     * Restituisce identificativo.
     * @return identificativo
     */
    public String getId() {
        return slotId;
    }

    /**
     * Imposta identificativo.
     *
     * @param id identificativo della risorsa
     */
    public void setId(String id) {
        this.slotId = id;
    }

    /**
     * Restituisce numero del posto.
     * @return numero del posto
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Imposta numero del posto.
     *
     * @param numero numero progressivo del posto
     */
    public void setNumero(int numero) {
        this.numero = numero;
    }

    /**
     * Restituisce nome.
     * @return nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta nome.
     *
     * @param nome nome descrittivo
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce numero del piano.
     * @return numero del piano
     */
    public int getPiano() {
        return piano;
    }

    /**
     * Imposta numero del piano.
     *
     * @param piano piano da filtrare o modificare
     */
    public void setPiano(int piano) {
        this.piano = piano;
    }

    /**
     * Restituisce tipo del posto.
     * @return tipo del posto
     */
    public TipoPosto getTipo() {
        return tipo;
    }

    /**
     * Imposta tipo del posto.
     *
     * @param tipo categoria del posto
     */
    public void setTipo(TipoPosto tipo) {
        this.tipo = tipo == null ? TipoPosto.NORMALE : tipo;
    }

    /**
     * Restituisce stato operativo.
     * @return stato operativo
     */
    public StatoPosto getStato() {
        return stato;
    }

    /**
     * Imposta stato operativo.
     *
     * @param stato nuovo stato operativo
     */
    public void setStato(StatoPosto stato) {
        this.stato = stato == null ? StatoPosto.LIBERO : stato;
    }

    /**
     * Indica se stato di fuori servizio.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isFuoriServizio() {
        return fuoriServizio;
    }

    /**
     * Imposta stato di fuori servizio.
     *
     * @param fuoriServizio nuovo valore della messa fuori servizio
     */
    public void setFuoriServizio(boolean fuoriServizio) {
        this.fuoriServizio = fuoriServizio;
    }

    /**
     * Restituisce distanza dall'uscita.
     * @return distanza dall'uscita
     */
    public int getDistanzaUscita() {
        return distanzaUscita;
    }

    /**
     * Imposta distanza dall'uscita.
     *
     * @param distanzaUscita distanza dall'uscita
     */
    public void setDistanzaUscita(int distanzaUscita) {
        this.distanzaUscita = distanzaUscita;
    }

    // ---------------------------------------------------------------------
    // Alias compatibili con il vecchio dominio Posto
    // ---------------------------------------------------------------------

    /**
     * Indica se disponibilità.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isDisponibile() {
        return stato == StatoPosto.LIBERO && !fuoriServizio;
    }

    /**
     * Imposta disponibilità.
     *
     * @param disponibile nuovo valore della disponibilità
     */
    public void setDisponibile(boolean disponibile) {
        if (disponibile) {
            this.stato = StatoPosto.LIBERO;
        } else if (this.stato == StatoPosto.LIBERO) {
            this.stato = StatoPosto.PRENOTATO;
        }
    }

    /**
     * Indica se stato di disabilitazione.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isDisabilitato() {
        return fuoriServizio;
    }

    /**
     * Imposta stato di disabilitazione.
     *
     * @param disabilitato nuovo valore legacy di disabilitazione
     */
    public void setDisabilitato(boolean disabilitato) {
        this.fuoriServizio = disabilitato;
    }

    /**
     * Indica se riserva per disabili.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isRiservatoDisabili() {
        return tipo == TipoPosto.DISABILI;
    }

    /**
     * Imposta riserva per disabili.
     *
     * @param riservatoDisabili riserva per disabili
     */
    public void setRiservatoDisabili(boolean riservatoDisabili) {
        if (riservatoDisabili) {
            this.tipo = TipoPosto.DISABILI;
        } else if (this.tipo == TipoPosto.DISABILI) {
            this.tipo = TipoPosto.NORMALE;
        }
    }

    /**
     * Indica se riserva per donne in gravidanza.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isRiservatoIncinta() {
        return tipo == TipoPosto.INCINTA;
    }

    /**
     * Imposta riserva per donne in gravidanza.
     *
     * @param riservatoIncinta riserva per donne in gravidanza
     */
    public void setRiservatoIncinta(boolean riservatoIncinta) {
        if (riservatoIncinta) {
            this.tipo = TipoPosto.INCINTA;
        } else if (this.tipo == TipoPosto.INCINTA) {
            this.tipo = TipoPosto.NORMALE;
        }
    }
}
