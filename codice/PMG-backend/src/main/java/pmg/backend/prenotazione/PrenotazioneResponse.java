package pmg.backend.prenotazione;

import java.time.LocalDateTime;

public record PrenotazioneResponse(
    String id,
    String utenteId,
    String parcheggioId,
    LocalDateTime dataCreazione,
    String codiceQr,
    StatoPrenotazione stato,
    LocalDateTime dataIngresso,
    LocalDateTime dataUscita
    
) {}