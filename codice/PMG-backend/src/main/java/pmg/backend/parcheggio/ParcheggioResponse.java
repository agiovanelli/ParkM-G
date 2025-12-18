package pmg.backend.parcheggio;

public record ParcheggioResponse(
    String id,
    String nome,
    String area,
    int postiTotali,
    int postiDisponibili
) {}