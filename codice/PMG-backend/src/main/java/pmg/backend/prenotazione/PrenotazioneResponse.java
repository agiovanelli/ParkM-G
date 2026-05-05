package pmg.backend.prenotazione;

import java.time.LocalDateTime;

import pmg.backend.posto.PostoResponse;

/**
 * DTO utilizzato per restituire i dati di una prenotazione.
 *
 * Include le informazioni principali della prenotazione,
 * come posizione, capacità e stato.
 *
 * @param id identificativo della prenotazione
 * @param utenteId identificativo dell'utente associato alla prenotazione
 * @param parcheggioId identificativo del parcheggio associato alla prenotazione
 * @param dataCreazione data di creazione della prenotazione
 * @param codiceQr codice QR associato della prenotazione
 * @param stato stato della prenotazione
 * @param dataIngresso informazioni temporali dell'ingresso nel parcheggio
 * @param dataUscita informazioni temporali dell'uscita dal parcheggio
 * @param importoPagato importo pagato dall'utente relativo alla permanenza nel parcheggio
 * @param posto informazioni relative al posto bloccato dalla prenotazione
 * @param scadenzaArrivo momento temporale entro il quale deve essere validato l'ingresso nel parcheggio
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
) {}