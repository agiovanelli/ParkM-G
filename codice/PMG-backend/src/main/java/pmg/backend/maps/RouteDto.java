package pmg.backend.maps;

import java.util.List;

public record RouteDto(
    String polyline,                 // overview_polyline.points
    String summary,                  // routes[].summary
    String distanceText,             // legs[0].distance.text
    int distanceMeters,              // legs[0].distance.value  ✅ METRI REALI SU STRADA
    String durationText,             // legs[0].duration.text
    int durationSeconds,             // legs[0].duration.value
    String durationInTrafficText,    // legs[0].duration_in_traffic.text (se disponibile)
    Integer durationInTrafficSeconds,// legs[0].duration_in_traffic.value (se disponibile)
    List<StepDto> steps              // legs[0].steps
) {}