package pmg.backend.maps;

import java.util.List;

public record RouteDto(
    String polyline,                 // overview_polyline.points
    String summary,                  // routes[].summary (testuale)
    String distanceText,             // legs[0].distance.text
    String durationText,             // legs[0].duration.text
    String durationInTrafficText,    // legs[0].duration_in_traffic.text (se disponibile)
    List<StepDto> steps              // legs[0].steps
) {}
