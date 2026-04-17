package pmg.backend.prenotazione;

import java.util.List;

public interface PrenotazioneService {
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

    List<PrenotazioneResponse> getByParcheggio(String parcheggioId);

    PrenotazioneResponse validaIngresso(String codiceQr);

	PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId);
	
	double calcolaImporto(String prenotazioneId);
	
    PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo);
    
    PrenotazioneResponse validaUscita(String codiceQr);
    
    PrenotazioneResponse getPrenotazioneByQr(String codiceQr);
    
    PrenotazioneResponse confermaParcheggio(String prenotazioneId);
}
