package Utente;

public interface GestioneUtenti{
	
	public Utente registrazione(String nome, String cognome, String password, String email);
	public Utente login(String password, String email);
	public void logout();
	public void selezione();
}
