package pmg.backend.log;

import java.time.LocalDateTime;

/**
 * DTO utilizzato per ricevere i dati necessari alla creazione
 * di un nuovo log.
 *
 * @param analiticaId identificativo dell'analitica associata
 * @param tipo categoria del log
 * @param severita livello di severità del log
 * @param titolo titolo del log
 * @param descrizione descrizione del log
 * @param data data e ora del log
 */
public record LogRequest(
        String analiticaId,
        LogCategoria tipo,
        LogSeverità severita,
        String titolo,
        String descrizione,
        LocalDateTime data
       
) {}