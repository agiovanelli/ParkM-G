package pmg.backend.parcheggio;

import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.analitiche.Analitiche;
import pmg.backend.analitiche.AnaliticheRepository;
import pmg.backend.log.LogCategoria;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.log.LogSeverità;
import pmg.backend.maps.DirectionsResponseDto;
import pmg.backend.maps.MapsServiceImpl;
import pmg.backend.maps.RouteDto;
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import pmg.backend.utente.UtenteService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementazione del servizio per la gestione dei parcheggi.
 *
 * Gestisce la ricerca dei parcheggi, le prenotazioni,
 * l'assegnazione dei posti e la gestione dello stato di emergenza.
 */
@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    /** Logger per il tracciamento delle operazioni. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);

    /** Repository per l'accesso ai dati dei parcheggi. */
    private final ParcheggioRepository parcheggioRepository;

    /** Repository per l'accesso ai dati delle prenotazioni. */
    private final PrenotazioneRepository prenotazioneRepository;

    /** Servizio per la gestione dei log. */
    private final LogService logService;

    /** Repository per gli utenti. */
    private final UtenteRepository utenteRepository;

    /** Servizio per la gestione degli utenti. */
    private final UtenteService utenteService;

    /** Repository per le analitiche. */
    private final AnaliticheRepository analiticheRepository;

    /** Repository per i posti auto. */
    private final PostoRepository postoRepository;

    /** Servizio per il calcolo dei percorsi. */
    private final MapsServiceImpl mapsService;

    /** Messaggio standard per parcheggio non trovato. */
    private String parcheggioNonTrovato = "Parcheggio non trovato";

    /**
     * Crea una nuova istanza del servizio parcheggi.
     *
     * @param parcheggioRepository repository dei parcheggi
     * @param prenotazioneRepository repository delle prenotazioni
     * @param logService servizio per i log
     * @param postoRepository repository dei posti
     * @param utenteRepository repository degli utenti
     * @param utenteService servizio utenti
     * @param analiticheRepository repository analitiche
     * @param mapsService servizio mappe
     */
    public ParcheggioServiceImpl(
            ParcheggioRepository parcheggioRepository,
            PrenotazioneRepository prenotazioneRepository,
            LogService logService,
            PostoRepository postoRepository,
            UtenteRepository utenteRepository, 
            UtenteService utenteService,
            AnaliticheRepository analiticheRepository,
            MapsServiceImpl mapsService) {

        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logService = logService;
        this.utenteRepository = utenteRepository;
        this.utenteService = utenteService;
        this.analiticheRepository = analiticheRepository;
        this.postoRepository = postoRepository;
        this.mapsService = mapsService;
    }

    /** Tempo di fallback per validità arrivo (secondi). */
    private static final int FALLBACK_VALIDITA_ARRIVO_SECONDS = 10 * 60;

    /** Buffer aggiuntivo al tempo di percorrenza (secondi). */
    private static final int BUFFER_VALIDITA_ARRIVO_SECONDS = 10 * 60;

    /**
     * Calcola il tempo di validità fallback.
     *
     * @return secondi di validità
     */
    private int computeFallbackArrivalValiditySeconds() {
        return FALLBACK_VALIDITA_ARRIVO_SECONDS;
    }

    /**
     * Calcola il tempo di validità a partire da una route.
     *
     * @param route percorso calcolato
     * @return secondi di validità
     */
    private int computeArrivalValiditySecondsFromRoute(RouteDto route) {
        int travelSeconds = route.durationInTrafficSeconds() != null
                ? route.durationInTrafficSeconds()
                : route.durationSeconds();

        return travelSeconds + BUFFER_VALIDITA_ARRIVO_SECONDS;
    }

    /**
     * Calcola dinamicamente la validità dell'arrivo usando le mappe.
     *
     * @param originLat latitudine origine
     * @param originLng longitudine origine
     * @param destLat latitudine destinazione
     * @param destLng longitudine destinazione
     * @return secondi di validità
     */
    private int computeDynamicArrivalValiditySeconds(
            double originLat,
            double originLng,
            double destLat,
            double destLng
    ) {
        try {
            DirectionsResponseDto directions = mapsService.getDirections(originLat, originLng, destLat, destLng);

            if (directions == null || directions.routes() == null || directions.routes().isEmpty()) {
                return computeFallbackArrivalValiditySeconds();
            }

            RouteDto bestRoute = directions.routes().get(0);
            return computeArrivalValiditySecondsFromRoute(bestRoute);
        } catch (Exception e) {
            return computeFallbackArrivalValiditySeconds();
        }
    }

    /**
     * Ricerca parcheggi per area.
     *
     * @param area area geografica
     * @return lista parcheggi trovati
     */
    @Override
    public List<ParcheggioResponse> cercaPerArea(String area) {
        LOGGER.info("Ricerca parcheggi nell'area: {}", area);
        return parcheggioRepository.findByAreaContainingIgnoreCase(area)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Effettua una prenotazione.
     *
     * @param req richiesta di prenotazione
     * @return prenotazione creata
     */
    @Override
    @Transactional
    public PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req) {
        LOGGER.info("Tentativo di prenotazione: utente={}, parcheggio={}",
                req.utenteId(), req.parcheggioId());

        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException(parcheggioNonTrovato));

        if (parcheggio.isInEmergenza()) {
            throw new IllegalStateException("Parcheggio in emergenza");
        }

        Utente utente = utenteRepository.findById(req.utenteId())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Map<String, String> preferenze = utenteService.getPreferenze(utente.getId());

        Posto migliorPosto = assegnaPostoOttimale(req.parcheggioId(), preferenze);

        if (migliorPosto == null) {
            throw new IllegalStateException("Posti esauriti");
        }

        migliorPosto.setDisponibile(false);
        postoRepository.save(migliorPosto);

        parcheggio.setPostiDisponibili(Math.max(0, parcheggio.getPostiDisponibili() - 1));
        parcheggioRepository.save(parcheggio);

        String codiceQr = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Prenotazione entity = new Prenotazione(
                req.utenteId(),
                req.parcheggioId(),
                now,
                codiceQr
        );

        entity.setPosto(new PostoResponse(migliorPosto));

        int validitaArrivoSecondi;
        if (req.originLat() != null && req.originLng() != null) {
            validitaArrivoSecondi = computeDynamicArrivalValiditySeconds(
                    req.originLat(),
                    req.originLng(),
                    parcheggio.getLatitudine(),
                    parcheggio.getLongitudine()
            );
        } else {
            validitaArrivoSecondi = computeFallbackArrivalValiditySeconds();
        }

        entity.setScadenzaArrivo(now.plusSeconds(validitaArrivoSecondi));

        Prenotazione salvata = prenotazioneRepository.save(entity);

        return new PrenotazioneResponse(
                salvata.getId(),
                salvata.getUtenteId(),
                salvata.getParcheggioId(),
                salvata.getDataCreazione(),
                salvata.getCodiceQr(),
                salvata.getStato(),
                salvata.getDataIngresso(),
                salvata.getDataUscita(),
                salvata.getImportoPagato(),
                salvata.getPosto(),
                salvata.getScadenzaArrivo()
        );
    }

    /**
     * Imposta lo stato di emergenza di un parcheggio.
     *
     * @param parcheggioId identificativo parcheggio
     * @param stato stato emergenza
     * @param motivo motivo dell'emergenza
     */
    @Override
    @Transactional
    public void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo) {
        Parcheggio p = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException(parcheggioNonTrovato));

        p.setInEmergenza(stato);
        parcheggioRepository.save(p);

        if (stato) {
            String analiticaId = getAnaliticaIdByParcheggioId(parcheggioId);

            LogRequest logReq = new LogRequest(
                    analiticaId,
                    LogCategoria.ALLARME,
                    LogSeverità.CRITICO,
                    "BLOCCO EMERGENZA",
                    "Parcheggio " + p.getNome() + " chiuso. Motivo: " + (motivo != null ? motivo : "N/D"),
                    LocalDateTime.now()
            );

            logService.salvaLog(logReq);
            LOGGER.error("EMERGENZA ATTIVATA: {}", p.getNome());
        } else {
            LOGGER.info("Emergenza revocata: {}", p.getNome());
        }
    }

    /**
     * Ricerca parcheggi nelle vicinanze.
     *
     * @param lat latitudine
     * @param lng longitudine
     * @param radius raggio in metri
     * @return lista parcheggi ordinati per distanza
     */
    @Override
    public List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius) {
        LOGGER.info("Ricerca parcheggi vicini a lat={}, lng={}, raggio={} m", lat, lng, radius);

        List<Parcheggio> tutti = parcheggioRepository.findAll();

        return tutti.stream()
                .filter(p -> p.getLatitudine() != 0 && p.getLongitudine() != 0)
                .filter(p -> distanzaMetri(lat, lng, p.getLatitudine(), p.getLongitudine()) <= radius)
                .sorted((p1, p2) -> Double.compare(
                        distanzaMetri(lat, lng, p1.getLatitudine(), p1.getLongitudine()),
                        distanzaMetri(lat, lng, p2.getLatitudine(), p2.getLongitudine())
                ))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Assegna il posto migliore in base alle preferenze.
     *
     * @param parcheggioId identificativo parcheggio
     * @param preferenze preferenze utente
     * @return posto selezionato
     */
    @Override
    public Posto assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze) {
        List<Posto> posti = postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId);
        LOGGER.info("Preferenze utente: preferenzeUtente={}", preferenze);
        Posto posto = selezionaPostoOttimale(preferenze, posti);
        return posto == null ? null : posto;
    }

    /**
     * Recupera un parcheggio per ID.
     *
     * @param id identificativo
     * @return parcheggio trovato
     */
    @Override
    public ParcheggioResponse getById(String id) {
        Parcheggio p = parcheggioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(parcheggioNonTrovato));
        return toResponse(p);
    }

    /**
     * Recupera i posti di un parcheggio.
     *
     * @param parcheggioId identificativo parcheggio
     * @param piano piano opzionale
     * @return lista posti
     */
    @Override
    public List<PostoResponse> getPosti(String parcheggioId, Integer piano) {
        List<Posto> posti = (piano == null)
                ? postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId)
                : postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc(parcheggioId, piano);

        return posti.stream()
                .map(PostoResponse::new)
                .toList();
    }
    /**
     * Converte un'entità Parcheggio in un oggetto di risposta.
     *
     * @param p entità parcheggio
     * @return DTO contenente i dati del parcheggio
     */
    private ParcheggioResponse toResponse(Parcheggio p) {
        return new ParcheggioResponse(
                p.getId(),
                p.getNome(),
                p.getArea(),
                p.getPostiTotali(),
                p.getPostiDisponibili(),
                p.getLatitudine(),
                p.getLongitudine(),
                p.isInEmergenza()
        );
    }

    /**
     * Seleziona il posto ottimale in base alle preferenze dell'utente.
     *
     * Applica un sistema di punteggio considerando:
     * - preferenze per disabilità o gravidanza
     * - distanza dall'uscita
     * - disponibilità del posto
     *
     * @param preferenzeUtente mappa delle preferenze dell'utente
     * @param posti lista dei posti disponibili
     * @return posto ottimale oppure null se non disponibile
     */
    private Posto selezionaPostoOttimale(Map<String, String> preferenzeUtente, List<Posto> posti) {
        LOGGER.info("Preferenze utente: preferenzeUtente={}",
                preferenzeUtente);
        if (posti == null || posti.isEmpty()) {
            return null;
        }

        if (preferenzeUtente == null) {
            preferenzeUtente = Map.of();
        }
        
        boolean disabile = "Si".equalsIgnoreCase(preferenzeUtente.get("disabile"));
        boolean incinta = "Si".equalsIgnoreCase(preferenzeUtente.get("donnaIncinta"));
        int distanzaPreferita = parseIntOrDefault(preferenzeUtente.get("distanza"), 1);
        
        Posto migliorPosto = null;
        int punteggioMassimo = Integer.MIN_VALUE;

        for (Posto posto : posti) {
            if (posto == null || !posto.isDisponibile() || posto.isDisabilitato() || (disabile && !posto.isRiservatoDisabili()) || (!disabile && posto.isRiservatoDisabili()) || (incinta && !posto.isRiservatoIncinta()) || (!incinta && posto.isRiservatoIncinta())) continue;

            int punteggio = 0;

            if (disabile && posto.isRiservatoDisabili()) punteggio += 8;
            if (incinta && posto.isRiservatoIncinta()) punteggio += 5;

            int differenza = Math.abs(posto.getDistanzaUscita() - distanzaPreferita);
            punteggio -= differenza;

            if (migliorPosto == null || punteggio > punteggioMassimo) {
                punteggioMassimo = punteggio;
                migliorPosto = posto;
            }
        }

        return migliorPosto;
    }

    /**
     * Converte una stringa in intero restituendo un valore di default
     * in caso di errore o valore nullo.
     *
     * @param value stringa da convertire
     * @param defaultValue valore di default
     * @return valore intero convertito o default
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Calcola la distanza tra due coordinate geografiche utilizzando
     * la formula dell'Haversine.
     *
     * @param lat1 latitudine punto 1
     * @param lon1 longitudine punto 1
     * @param lat2 latitudine punto 2
     * @param lon2 longitudine punto 2
     * @return distanza in metri
     */
    private double distanzaMetri(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Recupera l'identificativo dell'analitica associata a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return identificativo dell'analitica
     * @throws IllegalArgumentException se non esiste un'analitica associata
     */
    private String getAnaliticaIdByParcheggioId(String parcheggioId) {
        Analitiche analitica = analiticheRepository.findByParcheggioId(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Analitica non trovata per parcheggioId: " + parcheggioId));

        return analitica.getId();
    }

    /**
     * Sincronizza le statistiche dei posti del parcheggio.
     *
     * @param parcheggioId identificativo parcheggio
     */
    @Override
    public void syncPostiStats(String parcheggioId) {
        long total = postoRepository.countByParcheggioId(parcheggioId);
        long available = postoRepository
                .countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse(parcheggioId);

        parcheggioRepository.findById(parcheggioId).ifPresent(parcheggio -> {
            parcheggio.setPostiTotali((int) total);
            parcheggio.setPostiDisponibili((int) available);
            parcheggioRepository.save(parcheggio);
        });
    }
}