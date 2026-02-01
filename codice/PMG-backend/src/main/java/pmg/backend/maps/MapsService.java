package pmg.backend.maps;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapsService {

  private final RestTemplate restTemplate;
  private final String googleDirectionsKey;
  private final String googleGeocodingKey;

  public MapsService(
      RestTemplateBuilder builder,
      @Value("${google.directions.key}") String googleDirectionsKey,
      @Value("${google.geocoding.key:}") String googleGeocodingKey
  ) {
    this.restTemplate = builder.build();
    this.googleDirectionsKey = googleDirectionsKey;
    this.googleGeocodingKey = (googleGeocodingKey != null && !googleGeocodingKey.isBlank())
        ? googleGeocodingKey
        : googleDirectionsKey; // fallback
  }

  public DirectionsResponseDto getDirections(double oLat, double oLng, double dLat, double dLng) {
    if (googleDirectionsKey == null || googleDirectionsKey.isBlank()) {
      throw new RuntimeException("google.directions.key mancante");
    }

    String url = UriComponentsBuilder
        .fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
        .queryParam("origin", oLat + "," + oLng)
        .queryParam("destination", dLat + "," + dLng)
        .queryParam("mode", "driving")
        .queryParam("departure_time", "now")
        .queryParam("traffic_model", "best_guess")
        .queryParam("alternatives", "true")
        .queryParam("language", "it")
        .queryParam("region", "IT")
        .queryParam("key", googleDirectionsKey)
        .toUriString();

    ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);

    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
      throw new RuntimeException("Directions HTTP " + resp.getStatusCode());
    }

    JsonNode root = resp.getBody();
    String status = root.path("status").asText();
    if (!"OK".equals(status)) {
      String msg = root.path("error_message").asText(status);
      throw new RuntimeException("Directions status=" + status + " msg=" + msg);
    }

    List<RouteDto> out = new ArrayList<>();

    for (JsonNode r : root.path("routes")) {
      String overviewPolyline = r.path("overview_polyline").path("points").asText("");
      String routeSummary = r.path("summary").asText("");

      JsonNode leg0 = (r.path("legs").isArray() && r.path("legs").size() > 0) ? r.path("legs").get(0) : null;

      String distText = "";
      String durText = "";
      String durTrafficText = "";
      List<StepDto> steps = new ArrayList<>();

      int distMeters = 0;
      int durSeconds = 0;
      Integer durTrafficSeconds = null;

      if (leg0 != null) {
        distText = leg0.path("distance").path("text").asText("");
        durText = leg0.path("duration").path("text").asText("");
        durTrafficText = leg0.path("duration_in_traffic").path("text").asText("");

        distMeters = leg0.path("distance").path("value").asInt(0);
        durSeconds = leg0.path("duration").path("value").asInt(0);

        JsonNode dit = leg0.path("duration_in_traffic");
        if (!dit.isMissingNode() && !dit.isNull()) {
          durTrafficSeconds = dit.path("value").asInt(0);
        }

        for (JsonNode s : leg0.path("steps")) {
          String html = s.path("html_instructions").asText("");
          String sDistText = s.path("distance").path("text").asText("");
          String sDurText = s.path("duration").path("text").asText("");
          String man = s.path("maneuver").asText("");
          String sPoly = s.path("polyline").path("points").asText("");

          JsonNode st = s.path("start_location");
          JsonNode en = s.path("end_location");

          // Se hai aggiornato StepDto con i valori numerici:
          int sDistMeters = s.path("distance").path("value").asInt(0);
          int sDurSeconds = s.path("duration").path("value").asInt(0);

          steps.add(new StepDto(
              html,
              sDistText,
              sDistMeters,      // <-- solo se hai cambiato StepDto
              sDurText,
              sDurSeconds,      // <-- solo se hai cambiato StepDto
              man,
              sPoly,
              st.path("lat").asDouble(), st.path("lng").asDouble(),
              en.path("lat").asDouble(), en.path("lng").asDouble()
          ));
        }
      }

      out.add(new RouteDto(
    		    overviewPolyline,
    		    routeSummary,
    		    distText,
    		    distMeters,
    		    durText,
    		    durSeconds,
    		    durTrafficText,
    		    durTrafficSeconds,
    		    steps
    		));
    }

    RouteDto best = null;
    for (RouteDto r : out) {
      if (best == null) { best = r; continue; }

      int bestT = (best.durationInTrafficSeconds() != null)
          ? best.durationInTrafficSeconds()
          : best.durationSeconds();

      int rT = (r.durationInTrafficSeconds() != null)
          ? r.durationInTrafficSeconds()
          : r.durationSeconds();

      if (rT > 0 && rT < bestT) best = r;
    }

    return new DirectionsResponseDto(List.of(best));
  }

  public GeocodeResponseDto geocode(String address) {
    if (address == null || address.isBlank()) {
      throw new RuntimeException("address mancante");
    }
    if (googleGeocodingKey == null || googleGeocodingKey.isBlank()) {
      throw new RuntimeException("google.geocoding.key (o directions.key) mancante");
    }

    String url = UriComponentsBuilder
        .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
        .queryParam("address", address) // UriComponentsBuilder fa encoding
        .queryParam("key", googleGeocodingKey)
        .toUriString();

    ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);

    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
      throw new RuntimeException("Geocode HTTP " + resp.getStatusCode());
    }

    JsonNode root = resp.getBody();
    String status = root.path("status").asText();
    if (!"OK".equals(status)) {
      // esempio: ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST...
      String msg = root.path("error_message").asText(status);
      throw new RuntimeException("Geocode status=" + status + " msg=" + msg);
    }

    List<GeocodeResultDto> out = new ArrayList<>();
    for (JsonNode r : root.path("results")) {
      JsonNode loc = r.path("geometry").path("location");
      double lat = loc.path("lat").asDouble();
      double lng = loc.path("lng").asDouble();
      String formatted = r.path("formatted_address").asText("");
      String placeId = r.path("place_id").asText("");

      List<String> types = new ArrayList<>();
      for (JsonNode t : r.path("types")) types.add(t.asText());

      out.add(new GeocodeResultDto(lat, lng, formatted, placeId, types));
    }

    return new GeocodeResponseDto(out);
  }
}
