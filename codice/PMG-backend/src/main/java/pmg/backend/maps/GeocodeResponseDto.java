package pmg.backend.maps;

import java.util.List;

/**
 * DTO utilizzato per restituire la risposta di geocodifica.
 *
 * @param results lista dei risultati ottenuti dalla geocodifica
 */
public record GeocodeResponseDto(List<GeocodeResultDto> results) {}