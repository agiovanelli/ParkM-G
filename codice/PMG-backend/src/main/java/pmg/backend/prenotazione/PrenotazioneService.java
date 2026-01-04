package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrenotazioneService {

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    public List<PrenotazioneResponse> getStoricoUtente(String utenteId) {
        List<Prenotazione> lista = prenotazioneRepository.findByUtenteId(utenteId);
        
        // Convertiamo la lista di Entity in lista di Response (DTO)
        return lista.stream().map(p -> new PrenotazioneResponse(
            p.getId(),
            p.getUtenteId(),
            p.getParcheggioId(),
            p.getOrario(),
            p.getCodiceQr()
        )).toList();
    }
}