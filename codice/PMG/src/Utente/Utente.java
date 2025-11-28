package Utente;

import java.util.ArrayList;
import java.util.Map;

public class Utente implements DatiUtenti, GestioneUtenti{
	String nome;
	String cognome;
	String username;
	String id;
	String password;
	String email;
	Map<ArrayList<String>, String> preferenze;
	
	public Utente(String n, String c, String p, String e){
		nome = n;
		cognome = c;
		password = p;
		email = e;
		
		username = n + "." + c;
	}
	
	public Utente(String p, String e) {
		password = p;
		email = e;
	}

	@Override
	public Utente registrazione(Utente u) {
		u = new Utente(u.nome, u.cognome, u.password, u.email);
		return u;
	}

	@Override
	public Utente login(Utente u) {
		u = new Utente(u.password, u.email);
		return u;
	}

	@Override
	public void logout(Utente u) {
		// TODO azione di logout
		
	}

	@Override
	public void selezione() {
		// TODO Azione di salvataggio delle preferenze
		
	}

	@Override
	public String getUsername(Utente u) {
		return u.username;
	}

	@Override
	public String getId(Utente u) {
		return u.id;
	}

	@Override
	public Map<ArrayList<String>, String> getPreferenze(Utente u) {
		return u.preferenze;
	}

	@Override
	public Utente registrazione(String nome, String cognome, String password, String email) {
		Utente newUser = new Utente(nome, cognome, password, email);
		return newUser;
	}

	@Override
	public Utente login(String password, String email) {
		Utente user = new Utente(password, email);
		return user;
	}

	@Override
	public void logout() {
		// TODO azione di logout
		
	}
	
	@Override
	public void setSelezione() {
		// TODO Azione di salvataggio delle preferenze
		
	}
}
