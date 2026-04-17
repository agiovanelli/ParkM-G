package pmg.backend.maps;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maps")
public class MapsController {

  private final MapsServiceImpl mapsService;

  public MapsController(MapsServiceImpl mapsService) {
    this.mapsService = mapsService;
  }

  @GetMapping("/directions")
  public DirectionsResponseDto directions(
      @RequestParam double oLat,
      @RequestParam double oLng,
      @RequestParam double dLat,
      @RequestParam double dLng
  ) {
    return mapsService.getDirections(oLat, oLng, dLat, dLng);
  }

  @GetMapping("/geocode")
  public GeocodeResponseDto geocode(@RequestParam String address) {
    return mapsService.geocode(address);
  }
}
