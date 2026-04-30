package pmg.backend.log;

import java.util.List;
import java.util.Optional;

/**
 * Servizio per la gestione dei log.
 *
 * Definisce le operazioni principali per salvare,
 * recuperare e filtrare i log.
 */
public interface LogService {

	 /**
 	 * Salva un nuovo log a partire dai dati ricevuti.
 	 *
 	 * @param request dati necessari per la creazione del log
 	 * @return log salvato
 	 */
 	Log salvaLog(LogRequest request);
	 
	 /**
 	 * Salva un log già esistente o modificato.
 	 *
 	 * @param log log da salvare
 	 * @return log salvato
 	 */
 	Log salvaLog1(Log log);
	 
	 /**
 	 * Recupera un log tramite il suo identificativo.
 	 *
 	 * @param id identificativo del log
 	 * @return log trovato, se presente
 	 */
 	Optional<Log> getLogById(String id);

	 /**
 	 * Recupera tutti i log associati a un'analitica.
 	 *
 	 * @param analiticaId identificativo dell'analitica
 	 * @return lista dei log associati
 	 */
 	List<Log> getLogByAnaliticaId(String analiticaId);

	 /**
 	 * Recupera i log associati a un'analitica filtrandoli per categoria.
 	 *
 	 * @param analiticaId identificativo dell'analitica
 	 * @param tipo categoria dei log da recuperare
 	 * @return lista dei log associati all'analitica e alla categoria indicate
 	 */
 	List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo);
}