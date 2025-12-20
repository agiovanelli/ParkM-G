package pmg.backend.parcheggio;

import java.util.List;
import pmg.backend.prenotazione.PrenotazioneRequest;  
import pmg.backend.prenotazione.PrenotazioneResponse; 

public interface ParcheggioService {

    List<ParcheggioResponse> cercaPerArea(String area);

    
    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);
}