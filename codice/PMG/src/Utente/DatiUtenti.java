package Utente;

import java.util.ArrayList;
import java.util.Map;

public interface DatiUtenti {
	
	public Utente registrazione(Utente u);
	public Utente login(Utente u);
	public void logout(Utente u);
	public void selezione();
	public String getUsername(Utente u);
	public String getId(Utente u);
	public Map<ArrayList<String>, String> getPreferenze(Utente u);
}
