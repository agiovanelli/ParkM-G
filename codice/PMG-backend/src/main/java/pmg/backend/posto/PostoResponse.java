package pmg.backend.posto;

/**
 * DTO utilizzato per restituire i dati logici e lo stato di un posto auto.
 *
 * Mantiene anche alcuni campi derivati compatibili con il precedente contratto
 * API, come disponibilità, disabilitazione e riserve per categorie specifiche.
 */
public class PostoResponse {

    /**
     * Identificativo univoco del documento.
     */
    private String id;
    /**
     * Identificativo logico stabile del posto.
     */
    private String slotId;
    /**
     * Identificativo del parcheggio associato.
     */
    private String parcheggioId;
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
    private TipoPosto tipo;
    /**
     * Stato operativo corrente.
     */
    private StatoPosto stato;
    /**
     * Indica se il posto è utilizzabile e libero.
     */
    private boolean disponibile;
    /**
     * Campo derivato che indica la messa fuori servizio.
     */
    private boolean disabilitato;
    /**
     * Indica se il posto è riservato a persone con disabilità.
     */
    private boolean riservatoDisabili;
    /**
     * Indica se il posto è riservato a donne in gravidanza.
     */
    private boolean riservatoIncinta;
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
    public PostoResponse() {
    }

    /**
     * Crea una nuova istanza di PostoResponse con i dati indicati.
     *
     * @param posto posto da convertire o aggiornare
     */
    public PostoResponse(Posto posto) {
        this(posto, null);
    }

    /**
     * Crea una nuova istanza di PostoResponse con i dati indicati.
     *
     * @param posto posto da convertire o aggiornare
     * @param parcheggioId identificativo del parcheggio
     */
    public PostoResponse(Posto posto, String parcheggioId) {
        if (posto == null) {
            return;
        }

        this.id = posto.getSlotId();
        this.slotId = posto.getSlotId();
        this.parcheggioId = parcheggioId;
        this.numero = posto.getNumero();
        this.nome = posto.getNome();
        this.piano = posto.getPiano();
        this.tipo = posto.getTipo();
        this.stato = posto.getStato();
        this.fuoriServizio = posto.isFuoriServizio();
        this.distanzaUscita = posto.getDistanzaUscita();
        aggiornaCampiDerivati();
    }

    /**
     * Aggiorna i campi di compatibilità derivati da tipo, stato e messa fuori servizio.
     */
    private void aggiornaCampiDerivati() {
        this.disponibile = this.stato == StatoPosto.LIBERO && !this.fuoriServizio;
        this.disabilitato = this.fuoriServizio;
        this.riservatoDisabili = this.tipo == TipoPosto.DISABILI;
        this.riservatoIncinta = this.tipo == TipoPosto.INCINTA;
    }

    /**
     * Restituisce identificativo.
     * @return identificativo
     */
    public String getId() {
        return id;
    }

    /**
     * Estrae l'identificativo logico del posto dalla prenotazione.
     * @return identificativo del posto o {@code null}
     */
    public String getSlotId() {
        return slotId;
    }

    /**
     * Restituisce identificativo del parcheggio.
     * @return identificativo del parcheggio
     */
    public String getParcheggioId() {
        return parcheggioId;
    }

    /**
     * Restituisce numero del posto.
     * @return numero del posto
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Restituisce nome.
     * @return nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * Restituisce numero del piano.
     * @return numero del piano
     */
    public int getPiano() {
        return piano;
    }

    /**
     * Restituisce tipo del posto.
     * @return tipo del posto
     */
    public TipoPosto getTipo() {
        return tipo;
    }

    /**
     * Restituisce stato operativo.
     * @return stato operativo
     */
    public StatoPosto getStato() {
        return stato;
    }

    /**
     * Indica se disponibilità.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isDisponibile() {
        return disponibile;
    }

    /**
     * Indica se stato di disabilitazione.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isDisabilitato() {
        return disabilitato;
    }

    /**
     * Indica se riserva per disabili.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isRiservatoDisabili() {
        return riservatoDisabili;
    }

    /**
     * Indica se riserva per donne in gravidanza.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isRiservatoIncinta() {
        return riservatoIncinta;
    }

    /**
     * Indica se stato di fuori servizio.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isFuoriServizio() {
        return fuoriServizio;
    }

    /**
     * Restituisce distanza dall'uscita.
     * @return distanza dall'uscita
     */
    public int getDistanzaUscita() {
        return distanzaUscita;
    }

    /**
     * Imposta disponibilità.
     *
     * @param disponibile nuovo valore della disponibilità
     */
    public void setDisponibile(boolean disponibile) {
        this.stato = disponibile ? StatoPosto.LIBERO : StatoPosto.PRENOTATO;
        aggiornaCampiDerivati();
    }

    /**
     * Imposta stato operativo.
     *
     * @param stato nuovo stato operativo
     */
    public void setStato(StatoPosto stato) {
        this.stato = stato == null ? StatoPosto.LIBERO : stato;
        aggiornaCampiDerivati();
    }

    /**
     * Imposta stato di fuori servizio.
     *
     * @param fuoriServizio nuovo valore della messa fuori servizio
     */
    public void setFuoriServizio(boolean fuoriServizio) {
        this.fuoriServizio = fuoriServizio;
        aggiornaCampiDerivati();
    }
}
