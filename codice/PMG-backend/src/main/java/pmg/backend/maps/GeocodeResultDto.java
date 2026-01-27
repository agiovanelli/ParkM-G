package pmg.backend.maps;

import java.util.List;

public record GeocodeResultDto(
    double lat,
    double lng,
    String formattedAddress,
    String placeId,
    List<String> types
) {}
