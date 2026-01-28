package pmg.backend.log;

import java.time.LocalDateTime;

public record LogRequest(
        String analiticaId,
        LogCategoria tipo,
        LogSeverità severita,
        String titolo,
        String descrizione,
        LocalDateTime data
       
) {}
