package pmg.backend.log;

import java.util.List;

public interface LogService {

	 Log salvaLog(Log logEvento);

	 List<Log> getLogByAnaliticaId(String analiticaId);

	 List<Log> getLogByAnaliticaIdAndTipo(String analiticaId, String tipo);
}
