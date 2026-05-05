package pmg.backend.parcheggio;

/**
 * DTO utilizzato per restituire i dati di un parcheggio.
 *
 * Include le informazioni principali del parcheggio,
 * come posizione, capacità e stato.
 *
 * @param id identificativo del parcheggio
 * @param nome nome del parcheggio
 * @param area area geografica del parcheggio
 * @param postiTotali numero totale di posti
 * @param postiDisponibili numero di posti disponibili
 * @param latitudine latitudine della posizione
 * @param longitudine longitudine della posizione
 * @param inEmergenza indica se il parcheggio è in stato di emergenza
 */
public record ParcheggioResponse(
        String id,
        String nome,
        String area,
        int postiTotali,
        int postiDisponibili,
        double latitudine,
        double longitudine,
        boolean inEmergenza
) {}