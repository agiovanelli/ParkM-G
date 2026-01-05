package pmg.backend.log;

import java.util.List;
import java.util.Optional;

public interface LogService {

	 Log salvaLog(LogRequest request);
	 
	 Log salvaLog1(Log log);
	 
	 Optional<Log> getLogById(String id);

	 List<Log> getLogByAnaliticaId(String analiticaId);

	 List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo);
}
