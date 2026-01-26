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
  private final String googleKey;

  public MapsService(RestTemplateBuilder builder,
                     @Value("${google.directions.key}") String googleKey) {
    this.restTemplate = builder.build();
    this.googleKey = googleKey;
  }

  public DirectionsResponseDto getDirections(double oLat, double oLng, double dLat, double dLng) {
    if (googleKey == null || googleKey.isBlank()) {
      throw new RuntimeException("google.directions.key mancante");
    }

    String url = UriComponentsBuilder
        .fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
        .queryParam("origin", oLat + "," + oLng)
        .queryParam("destination", dLat + "," + dLng)
        .queryParam("mode", "driving")
        .queryParam("departure_time", "now")        // abilita duration_in_traffic
        .queryParam("traffic_model", "best_guess")  // opzionale
        .queryParam("alternatives", "true")         // più percorsi
        .queryParam("key", googleKey)
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

      if (leg0 != null) {
        distText = leg0.path("distance").path("text").asText("");
        durText = leg0.path("duration").path("text").asText("");
        durTrafficText = leg0.path("duration_in_traffic").path("text").asText("");

        for (JsonNode s : leg0.path("steps")) {
          String html = s.path("html_instructions").asText("");
          String sDist = s.path("distance").path("text").asText("");
          String sDur = s.path("duration").path("text").asText("");
          String man = s.path("maneuver").asText(""); // spesso non c'è
          String sPoly = s.path("polyline").path("points").asText("");

          JsonNode st = s.path("start_location");
          JsonNode en = s.path("end_location");

          steps.add(new StepDto(
              html,
              sDist,
              sDur,
              man,
              sPoly,
              st.path("lat").asDouble(),
              st.path("lng").asDouble(),
              en.path("lat").asDouble(),
              en.path("lng").asDouble()
          ));
        }
      }

      out.add(new RouteDto(
          overviewPolyline,
          routeSummary,
          distText,
          durText,
          durTrafficText,
          steps
      ));
    }

    return new DirectionsResponseDto(out);
  }
}
