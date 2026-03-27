package pmg.backend.parcheggio;

import java.util.Map;

import pmg.backend.posto.Posto;

public record ParcheggioResponse(
	    String id,
	    String nome,
	    String area,
	    int postiTotali,
	    int postiDisponibili,
	    double latitudine,
	    double longitudine,
	    boolean inEmergenza,
	    Map<String, Map<String, Posto>> listaPosti
) {}
