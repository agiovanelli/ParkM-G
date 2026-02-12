package pmg.backend.prenotazione;

import java.util.List;

public interface PrenotazioneService {
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

	PrenotazioneResponse validaIngresso(String codiceQr);

	PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId);
	
	double calcolaImporto(String prenotazioneId);
    PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo);
    PrenotazioneResponse validaUscita(String codiceQr);
    
    PrenotazioneResponse getPrenotazioneByQr(String codiceQr);
    
}
