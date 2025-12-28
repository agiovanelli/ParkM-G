package pmg.backend.parcheggio;
import pmg.backend.prenotazione.PrenotazioneResponse;
import pmg.backend.prenotazione.PrenotazioneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.prenotazione.Prenotazione;
import pmg.backend.prenotazione.PrenotazioneRepository;

import java.util.List;
import java.util.UUID; // Per generare un codice QR temporaneo
import java.util.stream.Collectors;

@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);
    
    private final ParcheggioRepository parcheggioRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    // Aggiorna il costruttore per iniettare entrambi i repository
    public ParcheggioServiceImpl(ParcheggioRepository parcheggioRepository, PrenotazioneRepository prenotazioneRepository) {
        this.parcheggioRepository = parcheggioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @Override
    public List<ParcheggioResponse> cercaPerArea(String area) {
        LOGGER.info("Ricerca parcheggi nell'area: {}", area);
        return parcheggioRepository.findByAreaContainingIgnoreCase(area)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PrenotazioneResponse effettuaPrenotazione (PrenotazioneRequest req) { 
        LOGGER.info("Tentativo di prenotazione: utente={}, parcheggio={}", req.utenteId(), req.parcheggioId());
        // 1. Recupero il parcheggio
        Parcheggio parcheggio = parcheggioRepository.findById(req.parcheggioId())
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));

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
                req.orario(),
                codiceQr
        );
        Prenotazione salvata = prenotazioneRepository.save(entity);

        LOGGER.info("Prenotazione completata con successo! ID: {}", salvata.getId());

        return new PrenotazioneResponse(
                salvata.getId(),
                salvata.getUtenteId(),
                salvata.getParcheggioId(),
                salvata.getOrario(),
                salvata.getCodiceQr()
        );
    }

    private ParcheggioResponse toResponse(Parcheggio p) {
        return new ParcheggioResponse(
            p.getId(),
            p.getNome(),
            p.getArea(),
            p.getPostiTotali(),
            p.getPostiDisponibili(),
            p.getLatitudine(),
            p.getLongitudine()
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
                .collect(Collectors.toList());
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
}