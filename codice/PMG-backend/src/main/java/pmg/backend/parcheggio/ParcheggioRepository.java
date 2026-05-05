package pmg.backend.parcheggio;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * Repository per l'accesso ai dati del parcheggio.
 *
 * Fornisce metodi per recuperare il parcheggio in base
 * all'area in cui è localizzato.
 */
public interface ParcheggioRepository extends MongoRepository<Parcheggio, String> {
    
	/**
	 * Recupera i parcheggi che all'interno di un'area specifica.
	 *
	 * @param area area geografica
	 * @return lista dei parcheggi trovati
	 */
    List<Parcheggio> findByAreaContainingIgnoreCase(String area);
}