package Operatore;

import java.io.IOException;

public interface DatiOperatori {

	public String getNomeStruttura ();
	public String getUsername ();
	public boolean esisteOperatore(Operatore operatore) throws IOException;
}
