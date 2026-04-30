package pmg.backend.maps;

/**
 * Servizio per la gestione delle funzionalità Maps.
 *
 * Definisce le operazioni per ottenere percorsi
 * e risultati di geocodifica.
 */
public interface MapsService {
	
	/**
	 * Recupera i percorsi tra un punto di origine e una destinazione.
	 *
	 * @param oLat latitudine del punto di origine
	 * @param oLng longitudine del punto di origine
	 * @param dLat latitudine del punto di destinazione
	 * @param dLng longitudine del punto di destinazione
	 * @return risposta contenente i percorsi disponibili
	 */
	DirectionsResponseDto getDirections(double oLat, double oLng, double dLat, double dLng);
	
	/**
	 * Recupera i risultati di geocodifica per un indirizzo.
	 *
	 * @param address indirizzo da geocodificare
	 * @return risposta contenente i risultati della geocodifica
	 */
	GeocodeResponseDto geocode(String address);

}