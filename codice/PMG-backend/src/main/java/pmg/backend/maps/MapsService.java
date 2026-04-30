package pmg.backend.maps;

public interface MapsService {
	
	DirectionsResponseDto getDirections(double oLat, double oLng, double dLat, double dLng);
	
	GeocodeResponseDto geocode(String address);

}
