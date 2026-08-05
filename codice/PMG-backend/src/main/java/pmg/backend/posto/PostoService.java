package pmg.backend.posto;

import java.util.List;
import java.util.Map;

/**
 * Servizio per la gestione dei posti embedded nei documenti dei parcheggi.
 *
 * Definisce generazione, ricerca, aggiornamento dello stato, messa fuori servizio
 * e assegnazione del posto ottimale.
 */
public interface PostoService {

    /**
     * Restituisce posti by parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return posti by parcheggio
     */
    List<PostoResponse> getPostiByParcheggio(String parcheggioId);

    /**
     * Restituisce posti by parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @return posti by parcheggio
     */
    List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano);

    /**
     * Genera o completa i posti embedded sulla base della configurazione dei piani.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    void generaPosti(String parcheggioId);

    /**
     * Aggiorna la disponibilità di un posto embedded.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disponibile nuovo valore della disponibilità
     * @return posto aggiornato
     */
    PostoResponse aggiornaDisponibilita(
            String parcheggioId,
            int piano,
            int numero,
            boolean disponibile);

    /**
     * Aggiorna il campo legacy di disabilitazione del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disabilitato nuovo valore legacy di disabilitazione
     * @return posto aggiornato
     */
    PostoResponse aggiornaDisabilitato(
            String parcheggioId,
            int piano,
            int numero,
            boolean disabilitato);

    /**
     * Aggiorna la messa fuori servizio del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param fuoriServizio nuovo valore della messa fuori servizio
     * @return posto aggiornato
     */
    PostoResponse aggiornaFuoriServizio(
            String parcheggioId,
            String slotId,
            boolean fuoriServizio);

    /**
     * Aggiorna lo stato operativo del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param stato nuovo stato operativo
     * @return posto aggiornato
     */
    PostoResponse aggiornaStato(
            String parcheggioId,
            String slotId,
            StatoPosto stato);

    /**
     * Seleziona il posto ottimale, lo marca come prenotato e salva il parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param preferenze preferenze dell'utente
     * @return posto selezionato e marcato come prenotato
     */
    Posto prenotaPostoOttimale(String parcheggioId, Map<String, String> preferenze);

    /**
     * Ricerca un posto all'interno della configurazione embedded del parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @return posto trovato, se presente
     */
    Posto trovaPosto(String parcheggioId, String slotId);
}
