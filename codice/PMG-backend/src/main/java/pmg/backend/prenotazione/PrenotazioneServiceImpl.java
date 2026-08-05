package pmg.backend.prenotazione;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.exception.ConflictException;
import pmg.backend.log.LogCategoria;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.log.LogSeverità;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.posto.PostoResponse;
import pmg.backend.posto.PostoService;
import pmg.backend.posto.StatoPosto;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;

/**
 * Implementazione del servizio applicativo per la gestione delle prenotazioni.
 *
 * Gestisce validazione, annullamento, scadenza, pagamento, uscita e sincronizza
 * lo stato del posto embedded nel relativo parcheggio.
 */
@Service
public class PrenotazioneServiceImpl implements PrenotazioneService {

    /**
     * Repository per l'accesso alle prenotazioni.
     */
    private final PrenotazioneRepository prenotazioneRepository;
    /**
     * Repository per l'accesso ai parcheggi.
     */
    private final ParcheggioRepository parcheggioRepository;
    /**
     * Repository per l'accesso agli utenti.
     */
    private final UtenteRepository utenteRepository;
    /**
     * Servizio per la registrazione dei log.
     */
    private final LogService logService;
    /**
     * Repository per l'accesso alle analitiche.
     */
    private final AnaliticheRepository analiticheRepository;
    /**
     * Servizio per la gestione dei posti embedded.
     */
    private final PostoService postoService;

    /**
     * Messaggio standard utilizzato quando la prenotazione non viene trovata.
     */
    private final String prenotazioneNonTrovata =
            "Prenotazione non trovata";

    /** Clock utilizzato per le operazioni temporali e per i test deterministici. */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Crea una nuova istanza di PrenotazioneServiceImpl con i dati indicati.
     *
     * @param prenotazioneRepository prenotazione repository
     * @param parcheggioRepository parcheggio repository
     * @param utenteRepository utente repository
     * @param logService log service
     * @param analiticheRepository analitiche repository
     * @param postoService posto service
     */
    public PrenotazioneServiceImpl(
            PrenotazioneRepository prenotazioneRepository,
            ParcheggioRepository parcheggioRepository,
            UtenteRepository utenteRepository,
            LogService logService,
            AnaliticheRepository analiticheRepository,
            PostoService postoService) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.parcheggioRepository = parcheggioRepository;
        this.utenteRepository = utenteRepository;
        this.logService = logService;
        this.analiticheRepository = analiticheRepository;
        this.postoService = postoService;
    }

    /**
     * Recupera lo storico delle prenotazioni dell'utente.
     *
     * @param utenteId identificativo dell'utente
     * @return storico delle prenotazioni
     */
    @Override
    public List<PrenotazioneResponse> getStoricoUtente(String utenteId) {
        return prenotazioneRepository.findByUtenteId(utenteId).stream()
                .map(this::convertiInResponse)
                .toList();
    }

    /**
     * Recupera gli elementi associati al parcheggio indicato.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti del parcheggio
     */
    @Override
    public List<PrenotazioneResponse> getByParcheggio(String parcheggioId) {
        return prenotazioneRepository.findByParcheggioId(parcheggioId)
                .stream()
                .map(this::convertiInResponse)
                .toList();
    }

    /**
     * Individua periodicamente le prenotazioni scadute e libera i relativi posti.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void controllaPrenotazioniScadute() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Prenotazione> scadute =
                prenotazioneRepository.findByStatoAndScadenzaArrivoBefore(
                        StatoPrenotazione.attiva,
                        now);

        for (Prenotazione prenotazione : scadute) {
            liberaPostoPrenotazione(prenotazione);
            prenotazione.setStato(StatoPrenotazione.scaduta);
            aggiornaSnapshotPosto(prenotazione, StatoPosto.LIBERO);
            prenotazioneRepository.save(prenotazione);
        }
    }

    /**
     * Valida l'ingresso tramite il codice QR della prenotazione.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione aggiornata
     */
    @Override
    @Transactional
    public PrenotazioneResponse validaIngresso(String codiceQr) {
        Prenotazione prenotazione = prenotazioneRepository
                .findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException(
                        "QR Code non valido o inesistente"));

        if (prenotazione.getStato() != StatoPrenotazione.attiva) {
            throw new ConflictException(
                    "Prenotazione non valida: stato attuale = "
                            + prenotazione.getStato());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (prenotazione.getScadenzaArrivo() != null
                && now.isAfter(prenotazione.getScadenzaArrivo())) {
            liberaPostoPrenotazione(prenotazione);
            prenotazione.setStato(StatoPrenotazione.scaduta);
            aggiornaSnapshotPosto(prenotazione, StatoPosto.LIBERO);
            prenotazioneRepository.save(prenotazione);
            throw new ConflictException("Prenotazione scaduta");
        }

        prenotazione.setStato(StatoPrenotazione.inCorso);
        prenotazione.setDataIngresso(now);
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.VEICOLO,
                "Ingresso veicolo",
                "Ingresso validato per prenotazione " + salvata.getId());

        return convertiInResponse(salvata);
    }

    /**
     * Annulla una prenotazione e libera il posto associato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param utenteId identificativo dell'utente
     * @return prenotazione annullata
     */
    @Override
    @Transactional
    public PrenotazioneResponse annullaPrenotazione(
            String prenotazioneId,
            String utenteId) {
        Prenotazione prenotazione = prenotazioneRepository
                .findByIdAndUtenteId(prenotazioneId, utenteId)
                .orElseThrow(() -> new RuntimeException(
                        prenotazioneNonTrovata));

        if (prenotazione.getStato() != StatoPrenotazione.attiva
                && prenotazione.getStato() != StatoPrenotazione.inCorso
                && prenotazione.getStato() != StatoPrenotazione.parcheggiato) {
            throw new IllegalStateException(
                    "Puoi annullare solo prenotazioni ATTIVE, IN_CORSO o PARCHEGGIATO"
                            + " (stato attuale: "
                            + prenotazione.getStato()
                            + ")");
        }

        liberaPostoPrenotazione(prenotazione);
        prenotazione.setStato(StatoPrenotazione.annullata);
        aggiornaSnapshotPosto(prenotazione, StatoPosto.LIBERO);
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.INFO,
                "Prenotazione annullata",
                "La prenotazione " + salvata.getId() + " è stata annullata");

        return convertiInResponse(salvata);
    }

    /**
     * Calcola l'importo dovuto in base a durata, preferenze, fascia oraria e occupazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return importo calcolato
     */
    @Override
    public double calcolaImporto(String prenotazioneId) {
        Prenotazione prenotazione = prenotazioneRepository
                .findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException(
                        prenotazioneNonTrovata));

        if (prenotazione.getDataIngresso() == null) {
            return 0.0;
        }

        Utente utente = utenteRepository
                .findById(prenotazione.getUtenteId())
                .orElse(null);
        Parcheggio parcheggio = parcheggioRepository
                .findById(prenotazione.getParcheggioId())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now(clock);
        long durataMinuti = Duration.between(
                prenotazione.getDataIngresso(),
                now).toMinutes();
        durataMinuti = Math.max(1, durataMinuti);
        double durataOre = Math.ceil(durataMinuti / 60.0);
        double totale = durataOre * 3.0;

        if (utente != null && utente.getPreferenze() != null) {
            Map<String, String> prefs = utente.getPreferenze();
            String eta = prefs.get("eta");
            if ("under30".equals(eta)) {
                totale *= 0.90;
            } else if ("over60".equals(eta)) {
                totale *= 0.80;
            }
            if ("studente".equalsIgnoreCase(prefs.get("occupazione"))) {
                totale *= 0.85;
            }
        }

        int oraAttuale = now.getHour();
        if (oraAttuale >= 18 || oraAttuale <= 6) {
            totale *= 1.10;
        }

        DayOfWeek giorno = now.getDayOfWeek();
        if (giorno == DayOfWeek.SATURDAY
                || giorno == DayOfWeek.SUNDAY) {
            totale *= 1.20;
        }

        if (parcheggio != null && parcheggio.getPostiTotali() > 0) {
            double occupazione = 1.0
                    - ((double) parcheggio.getPostiDisponibili()
                    / parcheggio.getPostiTotali());
            long minutiAttesa = Duration.between(
                    prenotazione.getDataCreazione(),
                    prenotazione.getDataIngresso()).toMinutes();

            if (occupazione >= 0.80) {
                totale += minutiAttesa * 0.10;
            } else if (occupazione >= 0.50) {
                totale += minutiAttesa * 0.05;
            }
        }

        long attesaEffettiva = Duration.between(
                prenotazione.getDataCreazione(),
                prenotazione.getDataIngresso()).toMinutes();
        if (attesaEffettiva > 10) {
            totale += (attesaEffettiva - 10) * 0.5;
        }

        if (durataOre >= 24) {
            totale *= 0.50;
        } else if (durataOre >= 12) {
            totale *= 0.75;
        }

        return Math.round(totale * 100.0) / 100.0;
    }

    /**
     * Registra il pagamento e aggiorna lo stato della prenotazione.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @param importo importo da registrare
     * @return prenotazione aggiornata
     */
    @Override
    @Transactional
    public PrenotazioneResponse pagaPrenotazione(
            String prenotazioneId,
            double importo) {
        Prenotazione prenotazione = prenotazioneRepository
                .findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException(
                        prenotazioneNonTrovata));

        if (prenotazione.getStato() != StatoPrenotazione.parcheggiato) {
            throw new IllegalStateException(
                    "Puoi pagare solo una prenotazione PARCHEGGIATA. Stato attuale: "
                            + prenotazione.getStato());
        }
        if (importo <= 0) {
            throw new IllegalArgumentException("Importo non valido");
        }

        prenotazione.setImportoPagato(importo);
        prenotazione.setDataPagamento(LocalDateTime.now(clock));
        prenotazione.setStato(StatoPrenotazione.pagato);
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.PAGAMENTO,
                "Pagamento registrato",
                "Pagamento di €" + importo
                        + " registrato per prenotazione "
                        + salvata.getId());

        return convertiInResponse(salvata);
    }

    /**
     * Valida l'uscita e rende nuovamente libero il posto.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione conclusa
     */
    @Override
    @Transactional
    public PrenotazioneResponse validaUscita(String codiceQr) {
        Prenotazione prenotazione = prenotazioneRepository
                .findByCodiceQr(codiceQr)
                .orElseThrow(() -> new RuntimeException(
                        "QR Code non valido"));

        if (prenotazione.getStato() != StatoPrenotazione.pagato) {
            if (prenotazione.getStato() == StatoPrenotazione.inCorso
                    || prenotazione.getStato() == StatoPrenotazione.parcheggiato) {
                throw new IllegalStateException(
                        "Devi pagare prima di uscire");
            }
            throw new IllegalStateException(
                    "Stato non valido per l'uscita: "
                            + prenotazione.getStato());
        }

        LocalDateTime scadenzaUscita =
                prenotazione.getDataPagamento().plusMinutes(10);
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isAfter(scadenzaUscita)) {
            double fee = Math.max(
                    0,
                    Duration.between(scadenzaUscita, now).toMinutes() * 0.50);
            double importoCorrente = prenotazione.getImportoPagato() == null
                    ? 0.0
                    : prenotazione.getImportoPagato();
            prenotazione.setImportoPagato(importoCorrente + fee);
            prenotazioneRepository.save(prenotazione);
            throw new IllegalStateException(
                    "Tempo massimo per l'uscita scaduto. Fee aggiunta: €"
                            + fee);
        }

        liberaPostoPrenotazione(prenotazione);
        prenotazione.setStato(StatoPrenotazione.conclusa);
        prenotazione.setDataUscita(now);
        aggiornaSnapshotPosto(prenotazione, StatoPosto.LIBERO);
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.VEICOLO,
                "Uscita veicolo",
                "Uscita validata per prenotazione " + salvata.getId());

        return convertiInResponse(salvata);
    }

    /**
     * Recupera una prenotazione tramite il relativo codice QR.
     *
     * @param codiceQr codice QR della prenotazione
     * @return prenotazione trovata
     */
    @Override
    public PrenotazioneResponse getPrenotazioneByQr(String codiceQr) {
        return prenotazioneRepository.findByCodiceQr(codiceQr)
                .map(this::convertiInResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Prenotazione non trovata per il QR Code: "
                                + codiceQr));
    }

    /**
     * Conferma che il veicolo ha raggiunto e occupato il posto assegnato.
     *
     * @param prenotazioneId identificativo della prenotazione
     * @return prenotazione aggiornata
     */
    @Override
    @Transactional
    public PrenotazioneResponse confermaParcheggio(String prenotazioneId) {
        Prenotazione prenotazione = prenotazioneRepository
                .findById(prenotazioneId)
                .orElseThrow(() -> new RuntimeException(
                        prenotazioneNonTrovata));

        if (prenotazione.getStato() == StatoPrenotazione.parcheggiato) {
            return convertiInResponse(prenotazione);
        }
        if (prenotazione.getStato() != StatoPrenotazione.inCorso) {
            throw new IllegalStateException(
                    "Puoi confermare il parcheggio solo per prenotazioni IN_CORSO. Stato attuale: "
                            + prenotazione.getStato());
        }

        String slotId = getSlotId(prenotazione);
        postoService.aggiornaStato(
                prenotazione.getParcheggioId(),
                slotId,
                StatoPosto.OCCUPATO);

        prenotazione.setStato(StatoPrenotazione.parcheggiato);
        aggiornaSnapshotPosto(prenotazione, StatoPosto.OCCUPATO);
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        salvaLogEvento(
                salvata.getParcheggioId(),
                LogCategoria.EVENTO,
                LogSeverità.INFO,
                "Parcheggio confermato",
                "Utente arrivato al posto per prenotazione "
                        + salvata.getId());

        return convertiInResponse(salvata);
    }

    /**
     * Imposta il clock da utilizzare nelle operazioni temporali, principalmente per i test.
     *
     * @param fixedClock clock da utilizzare
     */
    @Override
    public void setClock(Clock fixedClock) {
        this.clock = fixedClock == null
                ? Clock.systemDefaultZone()
                : fixedClock;
    }

    /**
     * Libera il posto associato alla prenotazione, se presente.
     *
     * @param prenotazione prenotazione da elaborare
     */
    private void liberaPostoPrenotazione(Prenotazione prenotazione) {
        String slotId = getSlotId(prenotazione);
        if (slotId == null) {
            return;
        }

        postoService.aggiornaStato(
                prenotazione.getParcheggioId(),
                slotId,
                StatoPosto.LIBERO);
    }

    /**
     * Estrae l'identificativo logico del posto dalla prenotazione.
     *
     * @param prenotazione prenotazione da elaborare
     * @return identificativo del posto o {@code null}
     */
    private String getSlotId(Prenotazione prenotazione) {
        PostoResponse posto = prenotazione.getPosto();
        if (posto == null) {
            return null;
        }
        if (posto.getSlotId() != null && !posto.getSlotId().isBlank()) {
            return posto.getSlotId();
        }
        return posto.getId();
    }

    /**
     * Aggiorna lo stato del posto memorizzato come snapshot nella prenotazione.
     *
     * @param prenotazione prenotazione da elaborare
     * @param stato nuovo stato operativo
     */
    private void aggiornaSnapshotPosto(
            Prenotazione prenotazione,
            StatoPosto stato) {
        if (prenotazione.getPosto() != null) {
            prenotazione.getPosto().setStato(stato);
        }
    }

    /**
     * Converte una prenotazione nel relativo DTO di risposta.
     *
     * @param prenotazione prenotazione da elaborare
     * @return DTO della prenotazione
     */
    private PrenotazioneResponse convertiInResponse(Prenotazione prenotazione) {
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
                prenotazione.getScadenzaArrivo());
    }

    /**
     * Recupera l'identificativo dell'analitica associata al parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return identificativo dell'analitica
     */
    private String getAnaliticaIdByParcheggioId(String parcheggioId) {
        Analitiche analitica = analiticheRepository
                .findByParcheggioId(parcheggioId)
                .orElseThrow(() -> new RuntimeException(
                        "Analitica non trovata per parcheggioId: "
                                + parcheggioId));
        return analitica.getId();
    }

    /**
     * Registra un evento nelle analitiche associate al parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param categoria categoria
     * @param severita severita
     * @param titolo titolo
     * @param descrizione descrizione
     */
    private void salvaLogEvento(
            String parcheggioId,
            LogCategoria categoria,
            LogSeverità severita,
            String titolo,
            String descrizione) {
        logService.salvaLog(new LogRequest(
                getAnaliticaIdByParcheggioId(parcheggioId),
                categoria,
                severita,
                titolo,
                descrizione,
                LocalDateTime.now(clock)));
    }
}
