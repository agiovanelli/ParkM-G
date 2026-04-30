package pmg.backend.maps;

import java.util.List;

/**
 * DTO utilizzato per rappresentare un percorso.
 *
 * Contiene le informazioni principali del tragitto,
 * inclusi distanza, durata, traffico e passaggi intermedi.
 *
 * @param polyline rappresentazione codificata del percorso
 * @param summary descrizione sintetica del percorso
 * @param distanceText distanza in formato testuale
 * @param distanceMeters distanza espressa in metri
 * @param durationText durata in formato testuale
 * @param durationSeconds durata espressa in secondi
 * @param durationInTrafficText durata con traffico in formato testuale
 * @param durationInTrafficSeconds durata con traffico espressa in secondi
 * @param steps lista dei passaggi del percorso
 */
public record RouteDto(
    String polyline,    
    String summary,
    String distanceText,
    int distanceMeters,
    String durationText,
    int durationSeconds,
    String durationInTrafficText,
    Integer durationInTrafficSeconds,
    List<StepDto> steps
) {}