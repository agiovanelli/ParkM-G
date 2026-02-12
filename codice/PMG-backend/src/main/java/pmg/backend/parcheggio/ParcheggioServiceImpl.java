package pmg.backend.parcheggio;
import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.log.LogCategoria;
import pmg.backend.log.LogRequest;
import pmg.backend.log.LogService;
import pmg.backend.log.LogSeverità;
import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID; // Per generare un codice QR temporaneo

@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);
    
    private final ParcheggioRepository parcheggioRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final LogService logService;

    // Aggiorna il costruttore per iniettare entrambi i repository
    public ParcheggioServiceImpl(ParcheggioRepository parcheggioRepository, PrenotazioneRepository prenotazioneRepository, LogService logService) {
        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logService = logService;
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
    public PrenotazioneResponse effettuaPrenotazione (PrenotazioneRequest req) { 
        LOGGER.info("Tentativo di prenotazione: utente={}, parcheggio={}", req.utenteId(), req.parcheggioId());
        // 1. Recupero il parcheggio
        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));
        
        
        //controllo emergenza
        if (parcheggio.isInEmergenza()) {
            LOGGER.warn("Prenotazione negata: il parcheggio {} è in stato di emergenza", parcheggio.getNome());
            throw new IllegalStateException("Parcheggio temporaneamente chiuso per emergenza");
        }

        // Controllo disponibilità standard
        if (parcheggio.getPostiDisponibili() <= 0) {
            throw new IllegalStateException("Posti esauriti");
        }
        
        
        
        

        // 2. Controllo disponibilità 
        if (parcheggio.getPostiDisponibili() <= 0) {
            LOGGER.warn("Prenotazione fallita: posti esauriti per il parcheggio {}", parcheggio.getNome());
            throw new IllegalStateException("Posti esauriti");
        }

        // 3. Scalo il posto e aggiorno il parcheggio
        parcheggio.setPostiDisponibili(parcheggio.getPostiDisponibili() - 1);
        parcheggioRepository.save(parcheggio);

        // 4. Genero un codice per il QR 
        String codiceQr = UUID.randomUUID().toString();

        // 5. Creo e salvo la prenotazione nel DB 
        Prenotazione entity = new Prenotazione(
                req.utenteId(),
                req.parcheggioId(),
                LocalDateTime.now(),
                codiceQr
        );
        Prenotazione salvata = prenotazioneRepository.save(entity);

        LOGGER.info("Prenotazione completata con successo! ID: {}", salvata.getId());

        return new PrenotazioneResponse(
                String.valueOf(salvata.getId()), 
                salvata.getUtenteId(),
                salvata.getParcheggioId(),
                salvata.getDataCreazione(),
                salvata.getCodiceQr(),
                salvata.getStato(),          
                salvata.getDataIngresso(),   
                salvata.getDataUscita(),
                salvata.getImportoPagato()
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
            LogRequest logReq = new LogRequest(
                parcheggioId,                // analiticaId
                LogCategoria.ALLARME,        // tipo (Enum)
                LogSeverità.CRITICO,         // severita (Enum)
                "BLOCCO EMERGENZA",          // titolo
                "Parcheggio " + p.getNome() + " chiuso. Motivo: " + (motivo != null ? motivo : "N/D"), // descrizione
                LocalDateTime.now()          // data
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
            p.isInEmergenza()
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
    
    @Override
    public ParcheggioResponse getById(String id) {
        Parcheggio p = parcheggioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));
        return toResponse(p);
    }
}