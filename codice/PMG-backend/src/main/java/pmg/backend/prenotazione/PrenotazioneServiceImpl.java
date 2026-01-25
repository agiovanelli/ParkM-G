package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


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
    
    @Scheduled(fixedRate = 60000) // Esegue il controllo ogni minuto
    public void controllaPrenotazioniScadute() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(10);
        
        // Trova le prenotazioni ancora ATTIVE fatte più di 10 minuti fa
        List<Prenotazione> scadute = prenotazioneRepository.findByStatoAndDataCreazioneBefore(
            StatoPrenotazione.ATTIVA, limite
        );
        
        for (Prenotazione p : scadute) {
            p.setStato(StatoPrenotazione.SCADUTA);
            prenotazioneRepository.save(p);
            // Qui potresti anche loggare l'evento o liberare il posto nel parcheggio
        }
    }
    
 
    @Override
    public PrenotazioneResponse validaIngresso(String codiceQr) {
        // 1. Cerca la prenotazione tramite QR
        Prenotazione prenotazione = prenotazioneRepository.findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException("QR Code non valido o inesistente"));

        // 2. Verifica che sia ancora ATTIVA (non scaduta o già usata)
        if (prenotazione.getStato() != StatoPrenotazione.ATTIVA) {
            throw new RuntimeException("La prenotazione non è più valida (Stato: " + prenotazione.getStato() + ")");
        }

        // 3. Aggiorna lo stato e registra l'orario di ingresso
        prenotazione.setStato(StatoPrenotazione.IN_CORSO);
        prenotazione.setDataIngresso(LocalDateTime.now());

        // 4. Salva e ritorna la risposta
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);
        return convertiInResponse(salvata);
    }

    // Metodo helper per evitare ripetizioni (usalo anche nel metodo dello storico)
    private PrenotazioneResponse convertiInResponse(Prenotazione p) {
        return new PrenotazioneResponse(
            p.getId(),
            p.getUtenteId(),
            p.getParcheggioId(),
            p.getDataCreazione(),
            p.getCodiceQr(),
            p.getStato(),
            p.getDataIngresso(),
            p.getDataUscita()
        );
    }
}
