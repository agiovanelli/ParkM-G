package pmg.backend.posto;

import java.util.List;

/**
 * Servizio per la gestione dei posti auto.
 *
 * Definisce le operazioni principali per la generazione,
 * consultazione e aggiornamento dello stato dei posti.
 */
public interface PostoService {

    /**
     * Recupera tutti i posti di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti
     */
    List<PostoResponse> getPostiByParcheggio(String parcheggioId);

    /**
     * Recupera i posti di un parcheggio filtrati per piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare (opzionale)
     * @return lista dei posti
     */
    List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano);

    /**
     * Genera automaticamente i posti per un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    void generaPosti(String parcheggioId);

    /**
     * Aggiorna la disponibilità di un posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disponibile nuovo stato di disponibilità
     * @return posto aggiornato
     */
    PostoResponse aggiornaDisponibilita(String parcheggioId, int piano, int numero, boolean disponibile);

    /**
     * Aggiorna lo stato di disabilitazione di un posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disabilitato nuovo stato di disabilitazione
     * @return posto aggiornato
     */
    PostoResponse aggiornaDisabilitato(String parcheggioId, int piano, int numero, boolean disabilitato);
}