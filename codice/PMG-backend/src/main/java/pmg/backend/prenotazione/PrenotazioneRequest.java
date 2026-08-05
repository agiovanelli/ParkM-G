package pmg.backend.prenotazione;

import java.time.LocalDateTime;

/**
 * DTO utilizzato per richiedere la creazione di una prenotazione.
 *
 * Include gli identificativi di utente e parcheggio e, quando disponibili, le
 * coordinate di origine usate per calcolare dinamicamente la scadenza di arrivo.
 *
 * @param utenteId identificativo dell'utente
 * @param parcheggioId identificativo del parcheggio
 * @param dataCreazione data indicata dal client per la creazione
 * @param originLat latitudine di origine, se disponibile
 * @param originLng longitudine di origine, se disponibile
 */
public record PrenotazioneRequest(
        String utenteId,
        String parcheggioId,
        LocalDateTime dataCreazione,
        Double originLat,
        Double originLng
) {
}
