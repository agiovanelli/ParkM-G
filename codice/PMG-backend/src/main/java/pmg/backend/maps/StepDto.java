package pmg.backend.maps;

/**
 * DTO utilizzato per rappresentare un singolo passaggio di un percorso.
 *
 * Contiene le istruzioni di navigazione, la distanza, la durata
 * e le coordinate di inizio e fine del passaggio.
 *
 * @param htmlInstructions istruzioni del passaggio in formato HTML
 * @param distanceText distanza in formato testuale
 * @param distanceMeters distanza espressa in metri
 * @param durationText durata in formato testuale
 * @param durationSeconds durata espressa in secondi
 * @param maneuver tipo di manovra prevista
 * @param polyline rappresentazione codificata del passaggio
 * @param startLat latitudine del punto iniziale
 * @param startLng longitudine del punto iniziale
 * @param endLat latitudine del punto finale
 * @param endLng longitudine del punto finale
 */
public record StepDto(
    String htmlInstructions,
    String distanceText,
    int distanceMeters,
    String durationText,
    int durationSeconds,
    String maneuver,
    String polyline,
    double startLat, 
    double startLng,
    double endLat, 
    double endLng
) {}