package Utente;

import java.util.Map;

public interface GestioneUtenti{
	
	public Utente registrazione(String nome, String cognome, String password, String email) throws Exception;
	public Utente login(String password, String email) throws Exception;
	public void logout();
	public void setSelezione(Map<String, String> p);
}
