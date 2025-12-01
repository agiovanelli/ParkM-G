package Utente;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
		id = UUID.randomUUID().toString();
	}
	
	public Utente(String p, String e) {
		password = p;
		email = e;
	}

	@Override
	public Utente registrazioneDB(Utente u) {
		//TODO aggiornamento database creando nuovo record
		return u;
	}

	@Override
	public void deleteDB(Utente u) {
		// TODO aggiornamento database eliminando record
	}

	@Override
	public void selezioneDB(Utente u, Map<ArrayList<String>, String> p) {
		// TODO Azione di salvataggio delle preferenze nel db
		
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
		registrazioneDB(newUser);
		return newUser;
	}

	@Override
	public Utente login(String password, String email) {
		Utente user = new Utente(password, email);
		return user;
	}

	@Override
	public void logout() {
		// TODO azione di logout su app
		
	}
	
	@Override
	public void setSelezione() {
		// TODO Azione di salvataggio delle preferenze
		selezioneDB(this, this.preferenze);
		
	}
}
