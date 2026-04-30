package pmg.backend.posto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostoServiceImpl implements PostoService {

    private final PostoRepository postoRepository;
    private final ParcheggioRepository parcheggioRepository;

    public PostoServiceImpl(PostoRepository postoRepository,
                            ParcheggioRepository parcheggioRepository) {
        this.postoRepository = postoRepository;
        this.parcheggioRepository = parcheggioRepository;
    }

    @Override
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
        return getPostiByParcheggio(parcheggioId, null);
    }

    @Override
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano) {
        List<Posto> posti = (piano == null)
                ? postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId)
                : postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc(parcheggioId, piano);

        return posti.stream()
                .map(PostoResponse::new)
                .toList();
    }

    @Override
    public void generaPosti(String parcheggioId) {
        List<Posto> esistenti = postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId);
        if (!esistenti.isEmpty()) {
            throw new IllegalStateException("Posti già generati per il parcheggio " + parcheggioId);
        }

        List<Posto> posti = new ArrayList<>();

        for (int piano = 1; piano <= 3; piano++) {
            for (int numero = 1; numero <= 18; numero++) {
                Posto p = new Posto();
                p.setPiano(piano);
                p.setNumero(numero);

                if (numero < 8) {
                    p.setDistanzaUscita(1);
                } else if (numero < 15) {
                    p.setDistanzaUscita(4);
                } else if (numero < 17) {
                    p.setDistanzaUscita(2);
                } else {
                    p.setDistanzaUscita(3);
                }

                p.setDisponibile(true);
                p.setDisabilitato(false);
                p.setRiservatoDisabili(numero >= 17);
                p.setRiservatoIncinta(numero >= 15 && numero <= 16);
                p.setParcheggioId(parcheggioId);

                posti.add(p);
            }
        }

        postoRepository.saveAll(posti);
    }

    @Override
    @Transactional
    public PostoResponse aggiornaDisponibilita(String parcheggioId, int piano, int numero, boolean disponibile) {
        Posto posto = postoRepository.findByParcheggioIdAndPianoAndNumero(parcheggioId, piano, numero)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        boolean oldDisponibile = posto.isDisponibile();

        posto.setDisponibile(disponibile);
        postoRepository.save(posto);

        if (oldDisponibile != disponibile && !posto.isDisabilitato()) {
            aggiornaContatoreDisponibili(parcheggioId, oldDisponibile, disponibile);
        }

        return new PostoResponse(posto);
    }

    @Override
    @Transactional
    public PostoResponse aggiornaDisabilitato(String parcheggioId, int piano, int numero, boolean disabilitato) {
        Posto posto = postoRepository.findByParcheggioIdAndPianoAndNumero(parcheggioId, piano, numero)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        boolean oldDisponibile = posto.isDisponibile();
        boolean oldDisabilitato = posto.isDisabilitato();

        posto.setDisabilitato(disabilitato);

        posto.setDisponibile(!disabilitato);

        postoRepository.save(posto);

        Parcheggio parcheggio = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));

        int disponibili = parcheggio.getPostiDisponibili();

        if (!oldDisabilitato && disabilitato && oldDisponibile) {
            disponibili--;
        } else if (oldDisabilitato && !disabilitato) {
            disponibili++;
        }

        parcheggio.setPostiDisponibili(
                Math.clamp(disponibili, 0, parcheggio.getPostiTotali())
        );
        parcheggioRepository.save(parcheggio);

        return new PostoResponse(posto);
    }

    private void aggiornaContatoreDisponibili(String parcheggioId, boolean oldDisponibile, boolean newDisponibile) {
        Parcheggio parcheggio = parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new RuntimeException("Parcheggio non trovato"));

        int disponibili = parcheggio.getPostiDisponibili();

        if (oldDisponibile && !newDisponibile) {
            disponibili--;
        } else if (!oldDisponibile && newDisponibile) {
            disponibili++;
        }

        parcheggio.setPostiDisponibili(Math.max(0, disponibili));
        parcheggioRepository.save(parcheggio);
    }
}