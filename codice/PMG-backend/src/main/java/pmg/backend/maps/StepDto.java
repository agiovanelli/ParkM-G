package pmg.backend.maps;

public record StepDto(
    String htmlInstructions,
    String distanceText,
    int distanceMeters,          // ✅ steps[].distance.value
    String durationText,
    int durationSeconds,         // ✅ steps[].duration.value
    String maneuver,
    String polyline,
    double startLat, double startLng,
    double endLat, double endLng
) {}