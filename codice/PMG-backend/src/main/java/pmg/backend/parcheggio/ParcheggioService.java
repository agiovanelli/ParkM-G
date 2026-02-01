package pmg.backend.parcheggio;

import java.util.List;
import pmg.backend.prenotazione.PrenotazioneRequest;  
import pmg.backend.prenotazione.PrenotazioneResponse; 

public interface ParcheggioService {

    List<ParcheggioResponse> cercaPerArea(String area);

    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);
    
    List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius);

	void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo);
    
	ParcheggioResponse getById(String id);
}