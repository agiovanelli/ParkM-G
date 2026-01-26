package pmg.backend.maps;

public record StepDto(
    String htmlInstructions,         // steps[].html_instructions (HTML)
    String distanceText,             // steps[].distance.text
    String durationText,             // steps[].duration.text
    String maneuver,                 // steps[].maneuver (può essere vuoto)
    String polyline,                 // steps[].polyline.points
    double startLat, double startLng,
    double endLat, double endLng
) {}
