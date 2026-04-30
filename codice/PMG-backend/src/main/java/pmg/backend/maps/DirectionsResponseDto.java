package pmg.backend.maps;

import java.util.List;

/**
 * DTO utilizzato per restituire la risposta contenente i percorsi.
 *
 * @param routes lista dei percorsi disponibili
 */
public record DirectionsResponseDto(List<RouteDto> routes) {}