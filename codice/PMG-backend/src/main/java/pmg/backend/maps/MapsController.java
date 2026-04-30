package pmg.backend.maps;

import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione delle funzionalità Maps.
 *
 * Espone endpoint per ottenere percorsi e coordinate
 * a partire da un indirizzo.
 */
@RestController
@RequestMapping("/api/maps")
public class MapsController {

  /** Servizio per la gestione delle operazioni Maps. */
  private final MapsServiceImpl mapsService;

  /**
   * Crea una nuova istanza del controller Maps.
   *
   * @param mapsService servizio per le operazioni Maps
   */
  public MapsController(MapsServiceImpl mapsService) {
    this.mapsService = mapsService;
  }

  /**
   * Recupera i percorsi tra un punto di origine e una destinazione.
   *
   * @param oLat latitudine del punto di origine
   * @param oLng longitudine del punto di origine
   * @param dLat latitudine del punto di destinazione
   * @param dLng longitudine del punto di destinazione
   * @return risposta contenente i percorsi disponibili
   */
  @GetMapping("/directions")
  public DirectionsResponseDto directions(
      @RequestParam double oLat,
      @RequestParam double oLng,
      @RequestParam double dLat,
      @RequestParam double dLng
  ) {
    return mapsService.getDirections(oLat, oLng, dLat, dLng);
  }

  /**
   * Recupera le coordinate associate a un indirizzo.
   *
   * @param address indirizzo da geocodificare
   * @return risposta contenente i risultati della geocodifica
   */
  @GetMapping("/geocode")
  public GeocodeResponseDto geocode(@RequestParam String address) {
    return mapsService.geocode(address);
  }
}