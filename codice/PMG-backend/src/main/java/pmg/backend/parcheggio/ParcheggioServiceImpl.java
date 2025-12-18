package pmg.backend.parcheggio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParcheggioServiceImpl implements ParcheggioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcheggioServiceImpl.class);
    private final ParcheggioRepository repository;

    public ParcheggioServiceImpl(ParcheggioRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ParcheggioResponse> cercaPerArea(String area) {
        LOGGER.info("Ricerca parcheggi nell'area: {}", area);
        
        return repository.findByAreaContainingIgnoreCase(area)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ParcheggioResponse toResponse(Parcheggio p) {
        return new ParcheggioResponse(
                p.getId(),
                p.getNome(),
                p.getArea(),
                p.getPostiTotali(),
                p.getPostiDisponibili()
        );
    }
}