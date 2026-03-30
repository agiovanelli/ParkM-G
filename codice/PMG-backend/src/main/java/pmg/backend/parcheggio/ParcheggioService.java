package pmg.backend.parcheggio;

import java.util.List;
import java.util.Map;

import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

public interface ParcheggioService {

    List<ParcheggioResponse> cercaPerArea(String area);

    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);

    List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius);

    void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo);

    PostoResponse assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze);

    ParcheggioResponse getById(String id);

    List<PostoResponse> getPosti(String parcheggioId, Integer piano);
}