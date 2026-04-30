package pmg.backend.maps;

import java.util.List;

/**
 * DTO utilizzato per rappresentare un risultato di geocodifica.
 *
 * @param lat latitudine del risultato
 * @param lng longitudine del risultato
 * @param formattedAddress indirizzo formattato
 * @param placeId identificativo del luogo
 * @param types tipologie associate al risultato
 */
public record GeocodeResultDto(
    double lat,
    double lng,
    String formattedAddress,
    String placeId,
    List<String> types
) {}