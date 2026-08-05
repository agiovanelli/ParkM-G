package pmg.backend.parcheggio;

/**
 * DTO sintetico utilizzato per restituire le informazioni principali di un parcheggio.
 *
 * Non contiene l'elenco completo dei posti e può quindi essere usato nelle
 * ricerche geografiche e nella visualizzazione dei marker senza appesantire la risposta.
 *
 * @param id identificativo univoco del parcheggio
 * @param nome nome del parcheggio
 * @param area area geografica del parcheggio
 * @param postiTotali numero totale di posti configurati
 * @param postiDisponibili numero di posti attualmente disponibili
 * @param numPiani numero di piani del parcheggio
 * @param latitudine latitudine del parcheggio
 * @param longitudine longitudine del parcheggio
 * @param inEmergenza indica se il parcheggio è in stato di emergenza
 */
public record ParcheggioResponse(
        String id,
        String nome,
        String area,
        int postiTotali,
        int postiDisponibili,
        int numPiani,
        double latitudine,
        double longitudine,
        boolean inEmergenza
) {
}
