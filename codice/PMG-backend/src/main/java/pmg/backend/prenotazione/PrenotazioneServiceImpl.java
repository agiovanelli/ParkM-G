package pmg.backend.prenotazione;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogCategoria;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.log.LogSeverità;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;

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
    private final LogService logService;
    private final AnaliticheRepository analiticheRepository;
    private final PostoRepository postoRepository;

    public PrenotazioneServiceImpl(
            PrenotazioneRepository prenotazioneRepository,
            ParcheggioRepository parcheggioRepository,
            UtenteRepository utenteRepository,
            LogService logService,
            AnaliticheRepository analiticheRepository,
            PostoRepository postoRepository) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.parcheggioRepository = parcheggioRepository;
        this.utenteRepository = utenteRepository;
        this.logService = logService;
        this.analiticheRepository = analiticheRepository;
        this.postoRepository = postoRepository;
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
                        p.getDataUscita(),
                        p.getImportoPagato(),
                        p.getPosto(),
                        p.getScadenzaArrivo()
                        )
                	)
                .toList();
    }
    
    @Scheduled(fixedRate = 60000)
    public void controllaPrenotazioniScadute() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(10);

        List<Prenotazione> scadute = prenotazioneRepository.findByStatoAndDataCreazioneBefore(
            StatoPrenotazione.ATTIVA, limite
        );

        for (Prenotazione p : scadute) {
            boolean postoLiberato = false;

            if (p.getPosto() != null && p.getPosto().getId() != null) {
                Posto posto = postoRepository
                    .findByIdAndParcheggioId(p.getPosto().getId(), p.getParcheggioId())
                    .orElse(null);

                if (posto != null && !posto.isDisponibile()) {
                    posto.setDisponibile(true);
                    postoRepository.save(posto);
                    postoLiberato = true;
                }
            }

            p.setStato(StatoPrenotazione.SCADUTA);
            if (p.getPosto() != null) {
                p.getPosto().setDisponibile(true);
            }
            prenotazioneRepository.save(p);

            if (postoLiberato) {
                parcheggioRepository.findById(p.getParcheggioId()).ifPresent(park -> {
                    int nuoviDisp = Math.min(park.getPostiTotali(), park.getPostiDisponibili() + 1);
                    park.setPostiDisponibili(nuoviDisp);
                    parcheggioRepository.save(park);
                });
            }
        }
    }
    
 
    @Override
    public PrenotazioneResponse validaIngresso(String codiceQr) {
        Prenotazione prenotazione = prenotazioneRepository.findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException("QR Code non valido o inesistente"));

        if (prenotazione.getStato() != StatoPrenotazione.ATTIVA) {
            throw new RuntimeException("La prenotazione non è più valida (Stato: " + prenotazione.getStato() + ")");
        }

        prenotazione.setStato(StatoPrenotazione.IN_CORSO);
        prenotazione.setDataIngresso(LocalDateTime.now());

        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.VEICOLO,
                "Ingresso veicolo",
                "Ingresso validato per prenotazione " + salvata.getId()
        );

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
            p.getDataUscita(),
            p.getImportoPagato(),
            p.getPosto(),
            p.getScadenzaArrivo()
        );
    }
    
    @Override
    @Transactional
    public PrenotazioneResponse annullaPrenotazione(String prenotazioneId, String utenteId) {
        Prenotazione p = prenotazioneRepository
            .findByIdAndUtenteId(prenotazioneId, utenteId)
            .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        if (p.getStato() != StatoPrenotazione.ATTIVA &&
        	    p.getStato() != StatoPrenotazione.IN_CORSO &&
        	    p.getStato() != StatoPrenotazione.PARCHEGGIATO) {
        	    throw new IllegalStateException(
        	        "Puoi annullare solo prenotazioni ATTIVE, IN_CORSO o PARCHEGGIATO (stato attuale: " + p.getStato() + ")"
        	    );
        	}

        boolean postoLiberato = false;

        // 1) libera il posto reale nella collection "posti"
        if (p.getPosto() != null && p.getPosto().getId() != null) {
            String postoId = p.getPosto().getId();

            Posto posto = postoRepository
                .findByIdAndParcheggioId(postoId, p.getParcheggioId())
                .orElseThrow(() -> new RuntimeException("Posto associato alla prenotazione non trovato"));

            if (!posto.isDisponibile()) {
                posto.setDisponibile(true);
                postoRepository.save(posto);
                postoLiberato = true;
            }
        }

        // 2) aggiorna contatore del parcheggio
        Parcheggio park = parcheggioRepository.findById(p.getParcheggioId())
            .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));

        if (postoLiberato) {
            int nuoviDisp = Math.min(park.getPostiTotali(), park.getPostiDisponibili() + 1);
            park.setPostiDisponibili(nuoviDisp);
            parcheggioRepository.save(park);
        }

        // 3) aggiorna stato prenotazione
        p.setStato(StatoPrenotazione.ANNULLATA);

        // aggiorna anche la copia del posto dentro la prenotazione
        if (p.getPosto() != null) {
            p.getPosto().setDisponibile(true);
        }

        Prenotazione salvata = prenotazioneRepository.save(p);
        
        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.INFO,
                "Prenotazione annullata",
                "La prenotazione " + salvata.getId() + " e' stata annullata"
        );

        return convertiInResponse(salvata);
    }
    
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
        if (durataMinuti < 1) {
            durataMinuti = 1;
        }
        double durataOre = Math.ceil(durataMinuti / 60.0);

        // 1. Costo Base (3€/ora)
        double totale = durataOre * 3.0;

        // 2. Modificatori Utente (Età e Occupazione)
        if (utente != null && utente.getPreferenze() != null) {
            Map<String, String> prefs = utente.getPreferenze();
            
            // Eta
            if (prefs.containsKey("eta")) {
                try {
                    String eta = prefs.get("eta");
                    if (eta.equals("under30")) totale *= 0.90; // Sconto 10% giovani
                    else if (eta.equals("over60")) totale *= 0.80; // Sconto 20% senior
                } catch (NumberFormatException ignored) {}
            }
            
            // Occupazione
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
            totale += (attesaEffettiva - 10) * 0.5;
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
    
    public double feePermanenza(LocalDateTime scadenzaUscita) {
    	double fee = Duration.between(scadenzaUscita, LocalDateTime.now()).toMinutes() * 1;

    	return fee;
    }

    @Override
    public PrenotazioneResponse pagaPrenotazione(String prenotazioneId, double importo) {
        Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        if (p.getStato() != StatoPrenotazione.PARCHEGGIATO) {
            throw new IllegalStateException("Puoi pagare solo prenotazioni IN CORSO. Stato attuale: " + p.getStato());
        }

        p.setImportoPagato(importo);
        p.setDataPagamento(LocalDateTime.now());
        p.setStato(StatoPrenotazione.PAGATO);

        Prenotazione salvata = prenotazioneRepository.save(p);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.PAGAMENTO,
                "Pagamento registrato",
                "Pagamento di €" + importo + " registrato per prenotazione " + salvata.getId()
        );

        return convertiInResponse(salvata);
    }

    @Override
    @Transactional
    public PrenotazioneResponse validaUscita(String codiceQr) {
        Prenotazione p = prenotazioneRepository.findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException("QR Code non valido"));

        if (p.getStato() != StatoPrenotazione.PAGATO) {
            if (p.getStato() == StatoPrenotazione.IN_CORSO) {
                throw new IllegalStateException("Devi pagare prima di uscire!");
            }
            throw new IllegalStateException("Stato non valido per l'uscita: " + p.getStato());
        }

        LocalDateTime scadenzaUscita = p.getDataPagamento().plusMinutes(10);
        if (LocalDateTime.now().isAfter(scadenzaUscita)) {
        	p.setImportoPagato(feePermanenza(scadenzaUscita));
            throw new IllegalStateException("Tempo massimo per l'uscita scaduto! Decurtazione fee di permamenza.");
        }

        boolean postoLiberato = false;

        if (p.getPosto() != null && p.getPosto().getId() != null) {
            String postoId = p.getPosto().getId();

            Posto posto = postoRepository
                .findByIdAndParcheggioId(postoId, p.getParcheggioId())
                .orElseThrow(() -> new RuntimeException("Posto associato alla prenotazione non trovato"));

            if (!posto.isDisponibile()) {
                posto.setDisponibile(true);
                postoRepository.save(posto);
                postoLiberato = true;
            }
        }

        p.setStato(StatoPrenotazione.CONCLUSA);
        p.setDataUscita(LocalDateTime.now());

        if (p.getPosto() != null) {
            p.getPosto().setDisponibile(true);
        }

        Parcheggio park = parcheggioRepository.findById(p.getParcheggioId())
                .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));

        if (postoLiberato) {
            park.setPostiDisponibili(Math.min(park.getPostiTotali(), park.getPostiDisponibili() + 1));
            parcheggioRepository.save(park);
        }

        Prenotazione salvata = prenotazioneRepository.save(p);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.VEICOLO,
                "Uscita veicolo",
                "Uscita validata per prenotazione " + salvata.getId()
        );

        return convertiInResponse(salvata);
    }
    
    
    @Override
    public PrenotazioneResponse getPrenotazioneByQr(String codiceQr) {
        Prenotazione prenotazione = prenotazioneRepository.findByCodiceQr(codiceQr)
            .orElseThrow(() -> new RuntimeException("Prenotazione non trovata per il QR Code: " + codiceQr));
        
        return mapToResponse(prenotazione);
    }

    /**
     * Metodo helper per convertire Prenotazione in PrenotazioneResponse
     */
    private PrenotazioneResponse mapToResponse(Prenotazione prenotazione) {
        return new PrenotazioneResponse(
            prenotazione.getId(),
            prenotazione.getUtenteId(),
            prenotazione.getParcheggioId(),
            prenotazione.getDataCreazione(),
            prenotazione.getCodiceQr(),
            prenotazione.getStato(),
            prenotazione.getDataIngresso(),
            prenotazione.getDataUscita(),
            prenotazione.getImportoPagato(),
            prenotazione.getPosto(),
            prenotazione.getScadenzaArrivo()
        );
    }

	@Override
	public List<PrenotazioneResponse> getByParcheggio(String parcheggioId) {
	    return prenotazioneRepository.findByParcheggioId(parcheggioId)
	            .stream()
	            .map(this::toResponse)
	            .toList();
	}
	
	private PrenotazioneResponse toResponse(Prenotazione p) {
	    return new PrenotazioneResponse(
	            String.valueOf(p.getId()),
	            p.getUtenteId(),
	            p.getParcheggioId(),
	            p.getDataCreazione(),
	            p.getCodiceQr(),
	            p.getStato(),
	            p.getDataIngresso(),
	            p.getDataUscita(),
	            p.getImportoPagato(),
	            p.getPosto(),
	            p.getScadenzaArrivo()
	    );
	}
	
	private String getAnaliticaIdByParcheggioId(String parcheggioId) {
	    Analitiche analitica = analiticheRepository.findByParcheggioId(parcheggioId)
	            .orElseThrow(() -> new RuntimeException(
	                    "Analitica non trovata per parcheggioId: " + parcheggioId));

	    return analitica.getId();
	}

	private void salvaLogEvento(
	        String parcheggioId,
	        LogCategoria categoria,
	        LogSeverità severita,
	        String titolo,
	        String descrizione) {

	    String analiticaId = getAnaliticaIdByParcheggioId(parcheggioId);

	    logService.salvaLog(new LogRequest(
	            analiticaId,
	            categoria,
	            severita,
	            titolo,
	            descrizione,
	            LocalDateTime.now()
	    ));
	}
	
	@Override
	public PrenotazioneResponse confermaParcheggio(String prenotazioneId) {
	    Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
	            .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

	    if (p.getStato() != StatoPrenotazione.IN_CORSO) {
	        if (p.getStato() == StatoPrenotazione.PARCHEGGIATO) {
	            return convertiInResponse(p);
	        }
	        throw new IllegalStateException(
	                "Puoi confermare il parcheggio solo per prenotazioni IN_CORSO. Stato attuale: " + p.getStato()
	        );
	    }

	    p.setStato(StatoPrenotazione.PARCHEGGIATO);

	    Prenotazione salvata = prenotazioneRepository.save(p);

	    salvaLogEvento(
	            salvata.getParcheggioId(),
	            LogCategoria.EVENTO,
	            LogSeverità.INFO,
	            "Parcheggio confermato",
	            "Utente arrivato al posto per prenotazione " + salvata.getId()
	    );

	    return convertiInResponse(salvata);
	}
}
