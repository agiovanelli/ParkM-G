package pmg.backend.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pmg.backend.exception.BadRequestException;
import pmg.backend.exception.MapsApiException;
import pmg.backend.exception.MapsConfigurationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MapsServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder builder;

    @InjectMocks
    private MapsServiceImpl mapsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(builder.build()).thenReturn(restTemplate);

        mapsService = new MapsServiceImpl(builder, "KEY", "KEY");
    }

    @Test
    void getDirections_bestRouteSelection() throws Exception {

        String json = """
        {
          "status": "OK",
          "routes": [
            {
              "summary": "Route 1",
              "overview_polyline": { "points": "aaa" },
              "legs": [{
                "distance": { "text": "10 km", "value": 10000 },
                "duration": { "text": "20 min", "value": 1200 }
              }]
            },
            {
              "summary": "Route 2",
              "overview_polyline": { "points": "bbb" },
              "legs": [{
                "distance": { "text": "8 km", "value": 8000 },
                "duration": { "text": "10 min", "value": 600 }
              }]
            }
          ]
        }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        DirectionsResponseDto res = mapsService.getDirections(1,1,2,2);

        assertEquals("Route 2", res.routes().get(0).summary());
    }
    
    @Test
    void getDirections_ok() throws Exception {

        String json = """
        {
          "status": "OK",
          "routes": [
            {
              "summary": "Test Route",
              "overview_polyline": { "points": "abc" },
              "legs": [
                {
                  "distance": { "text": "10 km", "value": 10000 },
                  "duration": { "text": "10 min", "value": 600 },
                  "duration_in_traffic": { "text": "12 min", "value": 720 },
                  "steps": []
                }
              ]
            }
          ]
        }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        DirectionsResponseDto res =
                mapsService.getDirections(1, 1, 2, 2);

        assertNotNull(res);
        assertEquals(1, res.routes().size());
        assertEquals("Test Route", res.routes().get(0).summary());
    }

    @Test
    void getDirections_noTrafficDuration() throws Exception {

        String json = """
        {
          "status": "OK",
          "routes": [
            {
              "summary": "No Traffic",
              "overview_polyline": { "points": "abc" },
              "legs": [{
                "distance": { "text": "10 km", "value": 10000 },
                "duration": { "text": "10 min", "value": 600 }
              }]
            }
          ]
        }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        DirectionsResponseDto res = mapsService.getDirections(1,1,2,2);

        assertNull(res.routes().get(0).durationInTrafficSeconds());
    }
    
    @Test
    void getDirections_statusError() throws Exception {
        String json = """
        { "status": "ZERO_RESULTS" }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        assertThrows(MapsApiException.class,
                () -> mapsService.getDirections(1,1,2,2));
    }

    @Test
    void getDirections_withSteps() throws Exception {

        String json = """
        {
          "status": "OK",
          "routes": [
            {
              "summary": "Route Steps",
              "overview_polyline": { "points": "abc" },
              "legs": [{
                "distance": { "text": "10 km", "value": 10000 },
                "duration": { "text": "10 min", "value": 600 },
                "steps": [
                  {
                    "html_instructions": "Vai dritto",
                    "distance": { "text": "1 km", "value": 1000 },
                    "duration": { "text": "1 min", "value": 60 },
                    "polyline": { "points": "xyz" },
                    "start_location": { "lat": 1.0, "lng": 2.0 },
                    "end_location": { "lat": 3.0, "lng": 4.0 }
                  }
                ]
              }]
            }
          ]
        }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        DirectionsResponseDto res = mapsService.getDirections(1,1,2,2);

        assertEquals(1, res.routes().get(0).steps().size());
    }
    
    @Test
    void getDirections_httpError() {
        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(MapsApiException.class,
                () -> mapsService.getDirections(1,1,2,2));
    }

    @Test
    void getDirections_noKey() {
        mapsService = new MapsServiceImpl(builder, "", "");

        assertThrows(MapsConfigurationException.class,
                () -> mapsService.getDirections(1,1,2,2));
    }

    @Test
    void geocode_ok() throws Exception {

        String json = """
        {
          "status": "OK",
          "results": [
            {
              "formatted_address": "Milano",
              "place_id": "abc123",
              "types": ["locality"],
              "geometry": {
                "location": { "lat": 45.46, "lng": 9.19 }
              }
            }
          ]
        }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        GeocodeResponseDto res = mapsService.geocode("Milano");

        assertEquals(1, res.results().size());
        assertEquals("Milano", res.results().get(0).formattedAddress());
    }

    @Test
    void geocode_emptyAddress() {
        assertThrows(BadRequestException.class,
                () -> mapsService.geocode(""));
    }

    @Test
    void geocode_statusError() throws Exception {
        String json = """
        { "status": "REQUEST_DENIED" }
        """;

        JsonNode node = objectMapper.readTree(json);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        MapsApiException ex = assertThrows(
                MapsApiException.class,
                () -> mapsService.geocode("Roma")
        );

        assertTrue(ex.getMessage().contains("REQUEST_DENIED"));
    }
}