package pmg.backend.analitiche;

/**
 * DTO utilizzato per ricevere i dati necessari alla creazione
 * di una nuova analitica.
 *
 * @param parcheggioId identificativo del parcheggio
 * @param nomeParcheggio nome del parcheggio
 * @param operatoreId identificativo dell'operatore
 */
public record AnaliticheRequest (
		String parcheggioId,
	    String nomeParcheggio,
	    String operatoreId
){}