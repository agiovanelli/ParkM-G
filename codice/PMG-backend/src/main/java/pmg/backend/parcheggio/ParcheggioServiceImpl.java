package pmg.backend.parcheggio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import pmg.backend.posto.PostoResponse;
import pmg.backend.posto.PostoService;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.prenotazione.PrenotazioneRequest;
import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import pmg.backend.utente.UtenteService;

/**
 * Implementazione del servizio applicativo per la gestione dei parcheggi.
 *
 * Coordina repository, prenotazioni, utenti, mappe, log e gestione dei posti
 * embedded nel documento {@link Parcheggio}.
 */
@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    /**
     * Logger utilizzato per tracciare le operazioni applicative.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParcheggioServiceImpl.class);

    /**
     * Durata di fallback, in secondi, concessa per raggiungere il parcheggio.
     */
    private static final int FALLBACK_VALIDITA_ARRIVO_SECONDS = 10 * 60;
    /**
     * Buffer aggiuntivo, in secondi, applicato al tempo stimato di percorrenza.
     */
    private static final int BUFFER_VALIDITA_ARRIVO_SECONDS = 10 * 60;

    /**
     * Repository per l'accesso ai parcheggi.
     */
    private final ParcheggioRepository parcheggioRepository;
    /**
     * Repository per l'accesso alle prenotazioni.
     */
    private final PrenotazioneRepository prenotazioneRepository;
    /**
     * Servizio per la registrazione dei log.
     */
    private final LogService logService;
    /**
     * Repository per l'accesso agli utenti.
     */
    private final UtenteRepository utenteRepository;
    /**
     * Servizio per la gestione degli utenti.
     */
    private final UtenteService utenteService;
    /**
     * Repository per l'accesso alle analitiche.
     */
    private final AnaliticheRepository analiticheRepository;
    /**
     * Servizio per la gestione dei posti embedded.
     */
    private final PostoService postoService;
    /**
     * Servizio utilizzato per calcolare i percorsi stradali.
     */
    private final MapsServiceImpl mapsService;

    /**
     * Messaggio standard utilizzato quando il parcheggio non viene trovato.
     */
    private final String parcheggioNonTrovato = "Parcheggio non trovato";

    /**
     * Crea una nuova istanza di ParcheggioServiceImpl con i dati indicati.
     *
     * @param parcheggioRepository parcheggio repository
     * @param prenotazioneRepository prenotazione repository
     * @param logService log service
     * @param postoService posto service
     * @param utenteRepository utente repository
     * @param utenteService utente service
     * @param analiticheRepository analitiche repository
     * @param mapsService maps service
     */
    public ParcheggioServiceImpl(
            ParcheggioRepository parcheggioRepository,
            PrenotazioneRepository prenotazioneRepository,
            LogService logService,
            PostoService postoService,
            UtenteRepository utenteRepository,
            UtenteService utenteService,
            AnaliticheRepository analiticheRepository,
            MapsServiceImpl mapsService) {
        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logService = logService;
        this.postoService = postoService;
        this.utenteRepository = utenteRepository;
        this.utenteService = utenteService;
        this.analiticheRepository = analiticheRepository;
        this.mapsService = mapsService;
    }

    /**
     * Cerca i parcheggi presenti nell'area indicata.
     *
     * @param area area geografica di ricerca
     * @return lista dei parcheggi trovati
     */
    @Override
    public List<ParcheggioResponse> cercaPerArea(String area) {
        return parcheggioRepository.findByAreaContainingIgnoreCase(area)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Assegna un posto idoneo e crea una nuova prenotazione.
     *
     * @param req dati della richiesta di prenotazione
     * @return prenotazione creata
     */
    @Override
    @Transactional
    public PrenotazioneResponse effettuaPrenotazione(PrenotazioneRequest req) {
        LOGGER.info(
                "Tentativo di prenotazione: utente={}, parcheggio={}",
                req.utenteId(),
                req.parcheggioId());

        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        parcheggioNonTrovato));

        if (parcheggio.isInEmergenza()) {
            throw new IllegalStateException("Parcheggio in emergenza");
        }

        Utente utente = utenteRepository.findById(req.utenteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utente non trovato"));

        Map<String, String> preferenze =
                utenteService.getPreferenze(utente.getId());

        Posto migliorPosto = assegnaPostoOttimale(
                req.parcheggioId(),
                preferenze);

        if (migliorPosto == null) {
            throw new IllegalStateException("Posti esauriti");
        }

        String codiceQr = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Prenotazione entity = new Prenotazione(
                req.utenteId(),
                req.parcheggioId(),
                now,
                codiceQr);

        entity.setPosto(new PostoResponse(migliorPosto, req.parcheggioId()));

        int validitaArrivoSecondi;
        if (req.originLat() != null && req.originLng() != null) {
            validitaArrivoSecondi = computeDynamicArrivalValiditySeconds(
                    req.originLat(),
                    req.originLng(),
                    parcheggio.getLatitudine(),
                    parcheggio.getLongitudine());
        } else {
            validitaArrivoSecondi = computeFallbackArrivalValiditySeconds();
        }

        entity.setScadenzaArrivo(
                now.plusSeconds(validitaArrivoSecondi));

        Prenotazione salvata = prenotazioneRepository.save(entity);
        return toPrenotazioneResponse(salvata);
    }

    /**
     * Aggiorna lo stato di emergenza e registra l'eventuale allarme.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param stato nuovo stato operativo
     * @param motivo motivo dell'emergenza, se disponibile
     */
    @Override
    @Transactional
    public void impostaStatoEmergenza(
            String parcheggioId,
            boolean stato,
            String motivo) {
        Parcheggio parcheggio = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        parcheggioNonTrovato));

        parcheggio.setInEmergenza(stato);
        parcheggioRepository.save(parcheggio);

        if (stato) {
            String analiticaId = getAnaliticaIdByParcheggioId(parcheggioId);
            logService.salvaLog(new LogRequest(
                    analiticaId,
                    LogCategoria.ALLARME,
                    LogSeverità.CRITICO,
                    "BLOCCO EMERGENZA",
                    "Parcheggio " + parcheggio.getNome()
                            + " chiuso. Motivo: "
                            + (motivo == null || motivo.isBlank()
                                ? "N/D"
                                : motivo),
                    LocalDateTime.now()));
        }
    }

    /**
     * Cerca e ordina i parcheggi vicini a una posizione geografica.
     *
     * @param lat latitudine del punto di ricerca
     * @param lng longitudine del punto di ricerca
     * @param radius raggio massimo di ricerca in metri
     * @return lista dei parcheggi ordinati per distanza
     */
    @Override
    public List<ParcheggioResponse> cercaVicini(
            double lat,
            double lng,
            double radius) {
        return parcheggioRepository.findAll().stream()
                .filter(p -> p.getLatitudine() != 0 && p.getLongitudine() != 0)
                .filter(p -> distanzaMetri(
                        lat,
                        lng,
                        p.getLatitudine(),
                        p.getLongitudine()) <= radius)
                .sorted((p1, p2) -> Double.compare(
                        distanzaMetri(
                                lat,
                                lng,
                                p1.getLatitudine(),
                                p1.getLongitudine()),
                        distanzaMetri(
                                lat,
                                lng,
                                p2.getLatitudine(),
                                p2.getLongitudine())))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Seleziona e prenota il posto più adatto alle preferenze dell'utente.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param preferenze preferenze dell'utente
     * @return posto assegnato oppure {@code null} se non disponibile
     */
    @Override
    public Posto assegnaPostoOttimale(
            String parcheggioId,
            Map<String, String> preferenze) {
        return postoService.prenotaPostoOttimale(
                parcheggioId,
                preferenze);
    }

    /**
     * Recupera un parcheggio tramite il relativo identificativo.
     *
     * @param id identificativo della risorsa
     * @return parcheggio richiesto
     */
    @Override
    public ParcheggioResponse getById(String id) {
        Parcheggio parcheggio = parcheggioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        parcheggioNonTrovato));
        return toResponse(parcheggio);
    }

    /**
     * Recupera la mappa logica completa del parcheggio.
     *
     * @param id identificativo della risorsa
     * @return mappa logica completa
     */
    @Override
    @Transactional
    public MappaParcheggioResponse getMappa(String id) {
        postoService.generaPosti(id);
        Parcheggio parcheggio = parcheggioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        parcheggioNonTrovato));
        return MappaParcheggioResponse.from(parcheggio);
    }

    /**
     * Recupera i posti di un parcheggio, eventualmente filtrati per piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @return elenco dei posti
     */
    @Override
    public List<PostoResponse> getPosti(
            String parcheggioId,
            Integer piano) {
        return postoService.getPostiByParcheggio(parcheggioId, piano);
    }

    /**
     * Sincronizza i contatori del parcheggio con lo stato dei posti embedded.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    @Override
    @Transactional
    public void syncPostiStats(String parcheggioId) {
        postoService.generaPosti(parcheggioId);
        Parcheggio parcheggio = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        parcheggioNonTrovato));
        parcheggio.ricalcolaStatistiche();
        parcheggioRepository.save(parcheggio);
    }

    /**
     * Converte un'entità nel relativo DTO di risposta.
     *
     * @param parcheggio documento del parcheggio da convertire o elaborare
     * @return DTO di risposta
     */
    private ParcheggioResponse toResponse(Parcheggio parcheggio) {
        int totaleConfigurato = parcheggio.getConfigurazionePiani().stream()
                .mapToInt(ConfigurazionePiano::getNumeroPosti)
                .sum();

        int totale = totaleConfigurato > 0
                ? totaleConfigurato
                : parcheggio.getPostiTotali();

        int disponibili = parcheggio.getPostiDisponibili();
        if (parcheggio.getTuttiPosti().isEmpty() && totale > 0) {
            disponibili = totale;
        }

        return new ParcheggioResponse(
                parcheggio.getId(),
                parcheggio.getNome(),
                parcheggio.getArea(),
                totale,
                disponibili,
                parcheggio.getNumPiani() > 0
                        ? parcheggio.getNumPiani()
                        : parcheggio.getConfigurazionePiani().size(),
                parcheggio.getLatitudine(),
                parcheggio.getLongitudine(),
                parcheggio.isInEmergenza());
    }

    /**
     * Converte una prenotazione nel relativo DTO di risposta.
     *
     * @param prenotazione prenotazione da elaborare
     * @return DTO della prenotazione
     */
    private PrenotazioneResponse toPrenotazioneResponse(
            Prenotazione prenotazione) {
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
     * Restituisce il tempo di validità di fallback per l'arrivo.
     * @return secondi di validità
     */
    private int computeFallbackArrivalValiditySeconds() {
        return FALLBACK_VALIDITA_ARRIVO_SECONDS;
    }

    /**
     * Calcola la validità dell'arrivo a partire dalla rotta restituita dal servizio mappe.
     *
     * @param route rotta restituita dal servizio mappe
     * @return secondi di validità calcolati
     */
    private int computeArrivalValiditySecondsFromRoute(RouteDto route) {
        int travelSeconds = route.durationInTrafficSeconds() != null
                ? route.durationInTrafficSeconds()
                : route.durationSeconds();
        return travelSeconds + BUFFER_VALIDITA_ARRIVO_SECONDS;
    }

    /**
     * Calcola dinamicamente il tempo concesso per raggiungere il parcheggio.
     *
     * @param originLat latitudine di origine
     * @param originLng longitudine di origine
     * @param destLat latitudine di destinazione
     * @param destLng longitudine di destinazione
     * @return secondi concessi per raggiungere il parcheggio
     */
    private int computeDynamicArrivalValiditySeconds(
            double originLat,
            double originLng,
            double destLat,
            double destLng) {
        try {
            DirectionsResponseDto directions = mapsService.getDirections(
                    originLat,
                    originLng,
                    destLat,
                    destLng);

            if (directions == null
                    || directions.routes() == null
                    || directions.routes().isEmpty()) {
                return computeFallbackArrivalValiditySeconds();
            }

            return computeArrivalValiditySecondsFromRoute(
                    directions.routes().get(0));
        } catch (Exception exception) {
            LOGGER.warn(
                    "Impossibile calcolare la validità dinamica dell'arrivo",
                    exception);
            return computeFallbackArrivalValiditySeconds();
        }
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Analitica non trovata per parcheggioId: "
                                + parcheggioId));
        return analitica.getId();
    }

    /**
     * Calcola la distanza in metri tra due coordinate mediante la formula dell'Haversine.
     *
     * @param lat1 latitudine del primo punto
     * @param lon1 longitudine del primo punto
     * @param lat2 latitudine del secondo punto
     * @param lon2 longitudine del secondo punto
     * @return distanza tra i due punti in metri
     */
    private double distanzaMetri(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {
        final int earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
