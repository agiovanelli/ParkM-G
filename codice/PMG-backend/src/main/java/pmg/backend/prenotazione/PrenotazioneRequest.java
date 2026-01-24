package pmg.backend.prenotazione;


public record PrenotazioneRequest(
    String utenteId,
    String parcheggioId
) {}