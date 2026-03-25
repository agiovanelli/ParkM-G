package pmg.backend.prenotazione;

import java.util.List;

import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.utente.Utente;

public interface PrenotazioneService {
    List<PrenotazioneResponse> getStoricoUtente(String utenteId);

	PrenotazioneResponse validaIngresso(String codiceQr);

	PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId);
	
	double calcolaImporto(String prenotazioneId);
    PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo);
    PrenotazioneResponse validaUscita(String codiceQr);
    
    PrenotazioneResponse selezionePostoOttimo(Utente utente, List<Parcheggio> listaPosti);
    
    PrenotazioneResponse getPrenotazioneByQr(String codiceQr);
    
}
