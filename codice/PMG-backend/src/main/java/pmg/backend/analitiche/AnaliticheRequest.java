package pmg.backend.analitiche;

public record AnaliticheRequest (
		String parcheggioId,
	    String nomeParcheggio,
	    String operatoreId
){}
