package Utente;

import java.util.ArrayList;
import java.util.Map;

import org.bson.types.ObjectId;

public interface DatiUtenti {
	public boolean registrazioneDB();
	public boolean loginDB();
	public boolean deleteDB();
	public boolean selezioneDB(Utente u, Map<String, String> p);
	public String getUsername();
	public ObjectId getId();
	public Object getPreferenze();
}
