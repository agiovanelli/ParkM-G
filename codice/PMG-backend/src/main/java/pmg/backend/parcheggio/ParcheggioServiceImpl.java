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
import pmg.backend.maps.MapsService;
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

@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);

    private final ParcheggioRepository parcheggioRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final LogService logService;
    private final UtenteRepository utenteRepository;
    private final UtenteService utenteService;
    private final AnaliticheRepository analiticheRepository;
    private final PostoRepository postoRepository;
    private final MapsService mapsService;

    public ParcheggioServiceImpl(
            ParcheggioRepository parcheggioRepository,
            PrenotazioneRepository prenotazioneRepository,
            LogService logService,
            PostoRepository postoRepository,
            UtenteRepository utenteRepository, 
            UtenteService utenteService,
            AnaliticheRepository analiticheRepository,
            MapsService mapsService) {

        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logService = logService;
        this.utenteRepository = utenteRepository;
        this.utenteService = utenteService;
        this.analiticheRepository = analiticheRepository;
        this.postoRepository = postoRepository;
        this.mapsService = mapsService;
    }
    
    private static final int FALLBACK_VALIDITA_ARRIVO_SECONDS = 10 * 60;
    private static final int BUFFER_VALIDITA_ARRIVO_SECONDS = 10 * 60;
    
    private int computeFallbackArrivalValiditySeconds() {
        return FALLBACK_VALIDITA_ARRIVO_SECONDS;
    }

    private int computeArrivalValiditySecondsFromRoute(RouteDto route) {
        int travelSeconds = route.durationInTrafficSeconds() != null
                ? route.durationInTrafficSeconds()
                : route.durationSeconds();

        return travelSeconds + BUFFER_VALIDITA_ARRIVO_SECONDS;
    }

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

    @Override
    public List<ParcheggioResponse> cercaPerArea(String area) {
        LOGGER.info("Ricerca parcheggi nell'area: {}", area);
        return parcheggioRepository.findByAreaContainingIgnoreCase(area)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req) {
        LOGGER.info("Tentativo di prenotazione: utente={}, parcheggio={}",
                req.utenteId(), req.parcheggioId());

        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));

        if (parcheggio.isInEmergenza()) {
            throw new IllegalStateException("Parcheggio in emergenza");
        }

        Utente utente = utenteRepository.findById(req.utenteId())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Map<String, String> preferenze = utenteService.getPreferenze(utente.getId());
        
        LOGGER.info("Preferenze utente: preferenzeUtente={}",
                preferenze);

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

    @Override
    @Transactional
    public void impostaStatoEmergenza(String parcheggioId, boolean stato, String motivo) {
        Parcheggio p = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));

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

    @Override
    public Posto assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze) {
        List<Posto> posti = postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId);
        LOGGER.info("Preferenze utente: preferenzeUtente={}",
                preferenze);
        Posto posto = selezionaPostoOttimale(preferenze, posti);
        return posto == null ? null : posto;
    }

    @Override
    public ParcheggioResponse getById(String id) {
        Parcheggio p = parcheggioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));
        return toResponse(p);
    }

    @Override
    public List<PostoResponse> getPosti(String parcheggioId, Integer piano) {
        List<Posto> posti = (piano == null)
                ? postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId)
                : postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc(parcheggioId, piano);

        return posti.stream()
                .map(PostoResponse::new)
                .toList();
    }

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
            if (posto == null) continue;
            if (!posto.isDisponibile()) continue;
            if (posto.isDisabilitato()) continue;

            if (disabile && !posto.isRiservatoDisabili()) continue;
            if (!disabile && posto.isRiservatoDisabili()) continue;
            if (incinta && !posto.isRiservatoIncinta()) continue;
            if (!incinta && posto.isRiservatoIncinta()) continue;

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

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

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

    private String getAnaliticaIdByParcheggioId(String parcheggioId) {
        Analitiche analitica = analiticheRepository.findByParcheggioId(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Analitica non trovata per parcheggioId: " + parcheggioId));

        return analitica.getId();
    }
    
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