package pmg.backend.prenotazione;

import java.time.LocalDateTime;

public record PrenotazioneRequest(
    String utenteId,
    String parcheggioId,
    LocalDateTime dataCreazione
) {}