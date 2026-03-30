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
import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;
import pmg.backend.posto.PostoResponse;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID; // Per generare un codice QR temporaneo

@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);
    
    private final ParcheggioRepository parcheggioRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final LogService logService;
    private final UtenteRepository utenteRepository;
    private final AnaliticheRepository analiticheRepository;
    private final PostoRepository postoRepository;
    
    // Aggiorna il costruttore per iniettare entrambi i repository
    public ParcheggioServiceImpl(
            ParcheggioRepository parcheggioRepository,
            PrenotazioneRepository prenotazioneRepository,
            LogService logService,
            PostoRepository postoRepository,
            UtenteRepository utenteRepository,
            AnaliticheRepository analiticheRepository) {

        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logService = logService;
        this.utenteRepository = utenteRepository;
        this.analiticheRepository = analiticheRepository;
        this.postoRepository = postoRepository;
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

        // 1️ Parcheggio
        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));

        if (parcheggio.isInEmergenza()) {
            throw new IllegalStateException("Parcheggio in emergenza");
        }

        // 2️ Utente
        Utente utente = utenteRepository.findById(req.utenteId())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Map<String, String> preferenze = utente.getPreferenze();

        // 3️ Mappe
        Map<String, Map<String, Posto>> listaPosti = parcheggio.getListaPosti();

        // 4️ Scansione posti
        SelectedPosto selected = assegnaPostoOttimale(preferenze, listaPosti);

        if (selected == null) {
            throw new IllegalStateException("Posti esauriti");
        }

        Posto migliorPosto = selected.posto();
        PostoResponse postoAssegnato = selected.response();

        // ora migliorPosto NON può essere null
        migliorPosto.setDisponibile(false);

        parcheggio.setPostiDisponibili(parcheggio.getPostiDisponibili() - 1);
        parcheggioRepository.save(parcheggio);

        String codiceQr = UUID.randomUUID().toString();

        Prenotazione entity = new Prenotazione(
                req.utenteId(),
                req.parcheggioId(),
                LocalDateTime.now(),
                codiceQr
        );

        entity.setPosto(postoAssegnato);

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
                salvata.getPosto()
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
            // Adattamento al tuo record LogRequest specifico
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
    
    private ParcheggioResponse toResponse(Parcheggio p) {
        return new ParcheggioResponse(
            p.getId(),
            p.getNome(),
            p.getArea(),
            p.getPostiTotali(),
            p.getPostiDisponibili(),
            p.getLatitudine(),
            p.getLongitudine(),
            p.isInEmergenza(),
            p.getListaPosti()
        );
    }
    
    @Override
    public List<ParcheggioResponse> cercaVicini(double lat, double lng, double radius) {
        LOGGER.info("Ricerca parcheggi vicini a lat={}, lng={}, raggio={} m", lat, lng, radius);

        // 1. Recupera tutti i parcheggi dal DB
        List<Parcheggio> tutti = parcheggioRepository.findAll();

        // 2. Calcola la distanza per ciascun parcheggio
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

    // Metodo di supporto per calcolare la distanza (formula haversine)
    private double distanzaMetri(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // raggio terrestre in metri
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
  
    
    private SelectedPosto assegnaPostoOttimale(
            Map<String, String> preferenzeUtente,
            Map<String, Map<String, Posto>> listaPosti) {

        if (listaPosti == null || listaPosti.isEmpty()) {
            return null;
        }

        if (preferenzeUtente == null) {
            preferenzeUtente = Map.of();
        }

        boolean disabile = Boolean.parseBoolean(
                preferenzeUtente.getOrDefault("disabile", "false"));

        boolean incinta = Boolean.parseBoolean(
                preferenzeUtente.getOrDefault("incinta", "false"));

        int distanzaPreferita = parseIntOrDefault(
                preferenzeUtente.get("distanzaPreferita"), 0);

        Posto migliorPosto = null;
        String migliorPianoKey = null;
        String migliorPostoKey = null;
        int punteggioMassimo = Integer.MIN_VALUE;

        for (Map.Entry<String, Map<String, Posto>> pianoEntry : listaPosti.entrySet()) {
            String pianoKey = pianoEntry.getKey();
            Map<String, Posto> postiPiano = pianoEntry.getValue();

            if (postiPiano == null || postiPiano.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Posto> postoEntry : postiPiano.entrySet()) {
                String postoKey = postoEntry.getKey();
                Posto posto = postoEntry.getValue();

                if (posto == null) continue;
                if (!posto.isDisponibile()) continue;

                if (disabile && !posto.isRiservatoDisabili()) continue;
                if (incinta && !posto.isRiservatoIncinta()) continue;

                int punteggio = 0;

                if (disabile && posto.isRiservatoDisabili()) {
                    punteggio += 50;
                }

                if (incinta && posto.isRiservatoIncinta()) {
                    punteggio += 30;
                }

                int differenza = Math.abs(posto.getDistanzaUscita() - distanzaPreferita);
                punteggio -= differenza;

                if (punteggio > punteggioMassimo) {
                    punteggioMassimo = punteggio;
                    migliorPosto = posto;
                    migliorPianoKey = pianoKey;
                    migliorPostoKey = postoKey;
                }
            }
        }

        if (migliorPosto == null) {
            return null;
        }

        int floor = extractFloorNumber(migliorPianoKey);
        String normalizedSlotNumber = normalizeSlotNumber(migliorPostoKey);
        String slotId = floor + "-" + normalizedSlotNumber;

        PostoResponse response = new PostoResponse(
                slotId,
                floor,
                normalizedSlotNumber,
                migliorPosto
        );

        return new SelectedPosto(migliorPosto, response);
    }
    
    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public ParcheggioResponse getById(String id) {
        Parcheggio p = parcheggioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));
        return toResponse(p);
    }
	
	public List<PostoResponse> getPosti(String parcheggioId, Integer piano) {
	    Parcheggio parcheggio = parcheggioRepository.findById(parcheggioId)
	            .orElseThrow(() -> new RuntimeException("Parcheggio non trovato: " + parcheggioId));

	    Map<String, Map<String, Posto>> listaPosti = parcheggio.getListaPosti();
	    List<PostoResponse> result = new ArrayList<>();

	    if (listaPosti == null || listaPosti.isEmpty()) {
	        return result;
	    }

	    for (Map.Entry<String, Map<String, Posto>> pianoEntry : listaPosti.entrySet()) {
	        String pianoKey = pianoEntry.getKey();      // es. "piano1"
	        int floor = extractFloorNumber(pianoKey);   // -> 1

	        if (piano != null && floor != piano) {
	            continue;
	        }

	        Map<String, Posto> postiDelPiano = pianoEntry.getValue();
	        if (postiDelPiano == null || postiDelPiano.isEmpty()) {
	            continue;
	        }

	        for (Map.Entry<String, Posto> postoEntry : postiDelPiano.entrySet()) {
	            String rawSlotNumber = postoEntry.getKey();   // es. "7" oppure "07"
	            Posto posto = postoEntry.getValue();

	            String normalizedSlotNumber = normalizeSlotNumber(rawSlotNumber);
	            String slotId = floor + "-" + normalizedSlotNumber;

	            result.add(new PostoResponse(
	                    slotId,
	                    floor,
	                    normalizedSlotNumber,
	                    posto
	            ));
	        }
	    }

	    result.sort(
	            Comparator.comparingInt(PostoResponse::getFloor)
	                    .thenComparing(PostoResponse::getSlotNumber)
	    );

	    return result;
	}

    private int extractFloorNumber(String pianoKey) {
        if (pianoKey == null || pianoKey.isBlank()) {
            throw new IllegalArgumentException("Chiave piano non valida: " + pianoKey);
        }

        String digits = pianoKey.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Impossibile estrarre numero piano da: " + pianoKey);
        }

        return Integer.parseInt(digits);
    }

    private String normalizeSlotNumber(String rawSlotNumber) {
        if (rawSlotNumber == null || rawSlotNumber.isBlank()) {
            throw new IllegalArgumentException("Numero posto non valido: " + rawSlotNumber);
        }

        int num = Integer.parseInt(rawSlotNumber);
        if (num <= 0) {
            throw new IllegalArgumentException("Numero posto non valido: " + rawSlotNumber);
        }

        return String.format("%02d", num);
    }

	@Override
	public PostoResponse assegnaPostoOttimale(String parcheggioId, Map<String, String> preferenze) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private record SelectedPosto(
			Posto posto,
		PostoResponse response
	) {}
	
	private String getAnaliticaIdByParcheggioId(String parcheggioId) {
	    Analitiche analitica = analiticheRepository.findByParcheggioId(parcheggioId)
	            .orElseThrow(() -> new IllegalArgumentException(
	                    "Analitica non trovata per parcheggioId: " + parcheggioId));

	    return analitica.getId();
	}
	
    @Override
    public PostoResponse aggiornaDisponibilita(String id, boolean disponibile) {
        Posto posto = postoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        posto.setDisponibile(disponibile);
        postoRepository.save(posto);

        return mapToResponse(posto);
    }

    private PostoResponse mapToResponse(Posto posto) {
        return new PostoResponse(posto);
    }

	@Override
	public PostoResponse aggiornaDisabilitato(String id, boolean disabilitato) {
		Posto posto = postoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        posto.setDisabilitato(disabilitato);
        posto.setDisponibile(false);
        postoRepository.save(posto);

        return mapToResponse(posto);
	}
}