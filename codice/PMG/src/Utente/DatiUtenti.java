package Utente;

import java.util.ArrayList;
import java.util.Map;

public interface DatiUtenti {
	
	public Utente registrazioneDB(Utente u);
	public void deleteDB(Utente u);
	public void selezioneDB(Utente u, Map<ArrayList<String>, String> p);
	public String getUsername(Utente u);
	public String getId(Utente u);
	public Map<ArrayList<String>, String> getPreferenze(Utente u);
}
