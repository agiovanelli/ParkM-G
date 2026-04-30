package pmg.backend.parcheggio;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;

@Service
public interface ParcheggioService {

    List<ParcheggioResponse> cercaPerArea(String area);

    PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req);

    List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius);

    void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo);

    Posto assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze);

    ParcheggioResponse getById(String id);

    List<PostoResponse> getPosti(String parcheggioId, Integer piano);

	void syncPostiStats(String parcheggioId);
}