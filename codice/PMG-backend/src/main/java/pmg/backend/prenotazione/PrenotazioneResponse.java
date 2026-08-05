package pmg.backend.prenotazione;

import java.time.LocalDateTime;

import pmg.backend.posto.PostoResponse;

/**
 * DTO utilizzato per restituire tutte le informazioni principali di una prenotazione.
 *
 * Comprende lo stato, le date del ciclo di vita, il pagamento e lo snapshot del
 * posto assegnato.
 *
 * @param id identificativo della prenotazione
 * @param utenteId identificativo dell'utente
 * @param parcheggioId identificativo del parcheggio
 * @param dataCreazione data e ora di creazione
 * @param codiceQr codice QR associato
 * @param stato stato corrente della prenotazione
 * @param dataIngresso data e ora di ingresso
 * @param dataUscita data e ora di uscita
 * @param importoPagato importo pagato
 * @param posto snapshot del posto assegnato
 * @param scadenzaArrivo termine entro il quale validare l'ingresso
 */
public record PrenotazioneResponse(
        String id,
        String utenteId,
        String parcheggioId,
        LocalDateTime dataCreazione,
        String codiceQr,
        StatoPrenotazione stato,
        LocalDateTime dataIngresso,
        LocalDateTime dataUscita,
        Double importoPagato,
        PostoResponse posto,
        LocalDateTime scadenzaArrivo
) {
}
