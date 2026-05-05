package pmg.backend.posto;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository per l'accesso ai dati dei posti auto.
 *
 * Fornisce metodi per recuperare e contare i posti
 * in base a parcheggio, piano e stato.
 */
@Repository
public interface PostoRepository extends MongoRepository<Posto, String> {

    /**
     * Recupera tutti i posti di un parcheggio ordinati per piano e numero.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return lista dei posti ordinati
     */
    List<Posto> findByParcheggioIdOrderByPianoAscNumeroAsc(String parcheggioId);

    /**
     * Recupera tutti i posti di un parcheggio per uno specifico piano.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del parcheggio
     * @return lista dei posti del piano ordinati per numero
     */
    List<Posto> findByParcheggioIdAndPianoOrderByNumeroAsc(String parcheggioId, int piano);

    /**
     * Recupera un posto specifico in base a parcheggio, piano e numero.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano del posto
     * @param numero numero del posto
     * @return posto trovato, se presente
     */
    Optional<Posto> findByParcheggioIdAndPianoAndNumero(String parcheggioId, int piano, int numero);
    
    /**
     * Recupera un posto tramite il suo identificativo e parcheggio associato.
     *
     * @param id identificativo del posto
     * @param parcheggioId identificativo del parcheggio
     * @return posto trovato, se presente
     */
    Optional<Posto> findByIdAndParcheggioId(String id, String parcheggioId);
    
    /**
     * Conta il numero totale di posti associati a un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return numero totale di posti
     */
    int countByParcheggioId(String parcheggioId);
    
    /**
     * Conta il numero di posti disponibili e non disabilitati per un parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return numero di posti disponibili
     */
    int countByParcheggioIdAndDisponibileTrueAndDisabilitatoFalse(String parcheggioId);
}