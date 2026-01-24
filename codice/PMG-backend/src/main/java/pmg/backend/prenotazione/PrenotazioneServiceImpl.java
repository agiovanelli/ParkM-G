package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PrenotazioneServiceImpl implements PrenotazioneService {

    private final PrenotazioneRepository prenotazioneRepository;

    public PrenotazioneServiceImpl(PrenotazioneRepository prenotazioneRepository) {
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @Override
    public List<PrenotazioneResponse> getStoricoUtente(String utenteId) {
        List<Prenotazione> lista = prenotazioneRepository.findByUtenteId(utenteId);

        // Convertiamo la lista di Entity in lista di Response (DTO)
        return lista.stream()
                .map(p -> new PrenotazioneResponse(
                        p.getId(),
                        p.getUtenteId(),
                        p.getParcheggioId(),
                        p.getDataCreazione(),
                        p.getCodiceQr(),
                        p.getStato(),
                        p.getDataIngresso(),
                        p.getDataUscita()
                ))
                .toList();
    }
}
