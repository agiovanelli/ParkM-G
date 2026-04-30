package pmg.backend.maps;

/**
 * DTO utilizzato per restituire le informazioni di un percorso.
 *
 * @param polyline rappresentazione codificata del percorso
 * @param summary descrizione sintetica del percorso
 */
public record DirectionsDto(String polyline, String summary) {}