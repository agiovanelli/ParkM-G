package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

import java.time.LocalDateTime;

import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import java.time.Duration;

import java.time.DayOfWeek;

import java.util.Map;


@Service
public class PrenotazioneServiceImpl implements PrenotazioneService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final ParcheggioRepository parcheggioRepository;
    private final UtenteRepository utenteRepository;

    public PrenotazioneServiceImpl(PrenotazioneRepository prenotazioneRepository, ParcheggioRepository parcheggioRepository,
            UtenteRepository utenteRepository) {
		this.prenotazioneRepository = prenotazioneRepository;
		this.parcheggioRepository = parcheggioRepository;
		this.utenteRepository = utenteRepository;
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
    
    @Transactional
    public PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId) {
        Prenotazione p = prenotazioneRepository
            .findByIdAndUtenteId(prenotazioneId, utenteId)
            .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        if (p.getStato() != StatoPrenotazione.ATTIVA && p.getStato() != StatoPrenotazione.IN_CORSO) {
            throw new IllegalStateException(
                "Puoi annullare solo prenotazioni ATTIVE (stato attuale: " + p.getStato() + ")"
            );
        }

        // 1) cambia stato
        p.setStato(StatoPrenotazione.ANNULLATA);
        Prenotazione salvata = prenotazioneRepository.save(p);

        // 2) libera posto nel parcheggio (con clamp a postiTotali)
        Parcheggio park = parcheggioRepository.findById(p.getParcheggioId())
            .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));

        int nuoviDisp = Math.min(park.getPostiTotali(), park.getPostiDisponibili() + 1);
        park.setPostiDisponibili(nuoviDisp);
        parcheggioRepository.save(park);

        return convertiInResponse(salvata);
    }
    
    
    //ALGORITMO DA CONTROLLARE
    @Override
    public double calcolaImporto(String prenotazioneId) {
        Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        if (p.getDataIngresso() == null) {
            return 0.0; // Non è ancora entrato
        }

        Utente utente = utenteRepository.findById(p.getUtenteId()).orElse(null);
        Parcheggio park = parcheggioRepository.findById(p.getParcheggioId()).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        long durataMinuti = Duration.between(p.getDataIngresso(), now).toMinutes();
        double durataOre = Math.ceil(durataMinuti / 60.0); // Arrotonda per eccesso

        // 1. Costo Base (3€/ora)
        double totale = durataOre * 3.0;

        // 2. Modificatori Utente (Età e Occupazione)
        if (utente != null && utente.getPreferenze() != null) {
            Map<String, String> prefs = utente.getPreferenze();
            
            // Esempio parsing età (salvata come stringa nelle preferenze)
            if (prefs.containsKey("eta")) {
                try {
                    int eta = Integer.parseInt(prefs.get("eta"));
                    if (eta < 25) totale *= 0.90; // Sconto 10% giovani
                    else if (eta > 65) totale *= 0.80; // Sconto 20% senior
                } catch (NumberFormatException ignored) {}
            }
            // Esempio occupazione
            if ("studente".equalsIgnoreCase(prefs.get("occupazione"))) {
                totale *= 0.85; // Sconto 15% studenti
            }
        }

        // 3. Modificatori Temporali (Fascia oraria e Tipo Giorno)
        int oraAttuale = now.getHour();
        if (oraAttuale >= 18 || oraAttuale <= 6) {
            totale *= 1.10; // +10% notturna
        }
        DayOfWeek giorno = now.getDayOfWeek();
        if (giorno == DayOfWeek.SATURDAY || giorno == DayOfWeek.SUNDAY) {
            totale *= 1.20; // +20% weekend
        }

        // 4. Fee Attesa QR (basata su occupazione parcheggio)
        if (park != null && park.getPostiTotali() > 0) {
            double occupazionePerc = 1.0 - ((double) park.getPostiDisponibili() / park.getPostiTotali());
            long minutiAttesa = Duration.between(p.getDataCreazione(), p.getDataIngresso()).toMinutes();
            
            double feeAttesa = 0.0;
            if (occupazionePerc >= 0.80) feeAttesa = minutiAttesa * 0.10;
            else if (occupazionePerc >= 0.50) feeAttesa = minutiAttesa * 0.05;
            
            totale += feeAttesa;
        }

        // 5. Penale ritardo convalida (> 10 min)
        long attesaEffettiva = Duration.between(p.getDataCreazione(), p.getDataIngresso()).toMinutes();
        if (attesaEffettiva > 10) {
            totale += (attesaEffettiva - 10) * 0.05;
        }

        // 6. Sconto durata complessiva
        if (durataOre >= 24) {
            totale *= 0.50; // -50%
        } else if (durataOre >= 12) {
            totale *= 0.75; // -25%
        }

        // Arrotondamento a 2 decimali
        return Math.round(totale * 100.0) / 100.0;
    }

    @Override
    public PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo) {
        Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        if (p.getStato() != StatoPrenotazione.IN_CORSO) {
            throw new IllegalStateException("Puoi pagare solo prenotazioni IN CORSO. Stato attuale: " + p.getStato());
        }

        p.setImportoPagato(importo);
        p.setDataPagamento(LocalDateTime.now());
        p.setStato(StatoPrenotazione.PAGATO); // CAMBIO STATO

        Prenotazione salvata = prenotazioneRepository.save(p);
        return convertiInResponse(salvata);
    }

    @Override
    @Transactional
    public PrenotazioneResponse validaUscita(String codiceQr) {
        Prenotazione p = prenotazioneRepository.findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException("QR Code non valido"));

        if (p.getStato() != StatoPrenotazione.PAGATO) {
            // Se è ancora in corso, deve pagare prima
            if (p.getStato() == StatoPrenotazione.IN_CORSO) {
                 throw new IllegalStateException("Devi pagare prima di uscire!");
            }
             throw new IllegalStateException("Stato non valido per l'uscita: " + p.getStato());
        }

        // Controllo timer 10 minuti dal pagamento
        LocalDateTime scadenzaUscita = p.getDataPagamento().plusMinutes(10);
        if (LocalDateTime.now().isAfter(scadenzaUscita)) {
            // Qui potresti resettare a IN_CORSO per far pagare la differenza, 
            // ma per ora lanciamo eccezione come da specifica
            throw new IllegalStateException("Tempo massimo per l'uscita scaduto! Contatta l'assistenza.");
        }

        // TUTTO OK: USCITA
        p.setStato(StatoPrenotazione.CONCLUSA);
        p.setDataUscita(LocalDateTime.now());
        
        // Libera il posto
        Parcheggio park = parcheggioRepository.findById(p.getParcheggioId())
                .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));
        
        park.setPostiDisponibili(Math.min(park.getPostiTotali(), park.getPostiDisponibili() + 1));
        parcheggioRepository.save(park);

        return convertiInResponse(prenotazioneRepository.save(p));
    }
}
