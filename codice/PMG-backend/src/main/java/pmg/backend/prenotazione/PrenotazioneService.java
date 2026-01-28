package pmg.backend.prenotazione;

import java.util.List;

public interface PrenotazioneService {
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

	PrenotazioneResponse validaIngresso(String codiceQr);

	PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId);
    
}
