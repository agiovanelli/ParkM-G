package pmg.backend.posto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione del servizio per la gestione dei posti auto.
 *
 * Gestisce il recupero, la generazione e l'aggiornamento
 * dello stato dei posti all'interno dei parcheggi.
 */
@Service
public class PostoServiceImpl implements PostoService {

    /** Repository per l'accesso ai dati dei posti. */
    private final PostoRepository postoRepository;

    /** Repository per l'accesso ai dati dei parcheggi. */
    private final ParcheggioRepository parcheggioRepository;

    /**
     * Crea una nuova istanza del servizio posti.
     *
     * @param postoRepository repository dei posti
     * @param parcheggioRepository repository dei parcheggi
     */
    public PostoServiceImpl(PostoRepository postoRepository,
                            ParcheggioRepository parcheggioRepository) {
        this.postoRepository = postoRepository;
        this.parcheggioRepository = parcheggioRepository;
    }

    /**
     * Recupera tutti i posti di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti
     */
    @Override
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
        return getPostiByParcheggio(parcheggioId, null);
    }

    /**
     * Recupera i posti di un parcheggio, opzionalmente filtrati per piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano opzionale
     * @return lista dei posti
     */
    @Override
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano) {
        List<Posto> posti = (piano == null)
                ? postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc(parcheggioId)
                : postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc(parcheggioId, piano);

        return posti.stream()
                .map(PostoResponse::new)
                .toList();
    }

    /**
     * Genera i posti per un parcheggio.
     *
     * Crea automaticamente i posti suddivisi per piano e numero,
     * assegnando distanza dall'uscita e vincoli (disabili, gravidanza).
     *
     * @param parcheggioId identificativo del parcheggio
     * @throws IllegalStateException se i posti sono già stati generati
     */
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

    /**
     * Aggiorna la disponibilità di un posto.
     *
     * Aggiorna lo stato del posto e sincronizza il contatore
     * dei posti disponibili del parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disponibile nuovo stato di disponibilità
     * @return posto aggiornato
     * @throws RuntimeException se il posto non viene trovato
     */
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

    /**
     * Aggiorna lo stato di disabilitazione di un posto.
     *
     * Imposta automaticamente la disponibilità in base allo stato
     * e aggiorna il contatore dei posti disponibili del parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @param disabilitato nuovo stato di disabilitazione
     * @return posto aggiornato
     * @throws RuntimeException se il posto o il parcheggio non vengono trovati
     */
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

    /**
     * Aggiorna il contatore dei posti disponibili di un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param oldDisponibile stato precedente
     * @param newDisponibile nuovo stato
     * @throws RuntimeException se il parcheggio non viene trovato
     */
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