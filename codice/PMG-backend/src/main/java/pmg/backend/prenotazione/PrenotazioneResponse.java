package pmg.backend.prenotazione;

import java.time.LocalDateTime;

import pmg.backend.posto.PostoResponse;

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
    PostoResponse posto
) {}