package pmg.backend.prenotazione;

import java.time.LocalDateTime;

/**
 * DTO utilizzato per restituire i dati di una prenotazione.
 *
 * Include gli identificativi di utente e parcheggio, la data di creazione
 * e le coordinate geografica.
 *
 * @param utenteId identificativo dell'utente associato alla prenotazione
 * @param parcheggioId identificativo del parcheggio associato alla prenotazione
 * @param dataCreazione data di creazione della prenotazione
 * @param originLat coordinate di latitudine
 * @param originLng coordinate di longitudine
 */
public record PrenotazioneRequest(
    String utenteId,
    String parcheggioId,
    LocalDateTime dataCreazione,
    Double originLat,
    Double originLng
) {}