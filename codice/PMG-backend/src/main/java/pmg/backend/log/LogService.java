package pmg.backend.log;

import java.util.List;
import java.util.Optional;

public interface LogService {

	 Log salvaLog(Log logEvento);
	 
	 Optional<Log> getLogById(String id);

	 List<Log> getLogByAnaliticaId(String analiticaId);

	 List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo);
}
