package Utente;

import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

public interface DatiUtenti {
	public boolean registrazioneDB();
	public boolean loginDB();
	public boolean deleteDB();
	public boolean selezioneDB(Map<String, String> preferenze);
	public Document controlloCredenziali(String email, String password);
	public String getUsername();
	public ObjectId getId();
	public Object getPreferenze();
}
