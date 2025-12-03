package Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import Database.Connessione;

public class Utente implements DatiUtenti, GestioneUtenti{
	String nome;
	String cognome;
	String username;
	String password;
	String email;
	ObjectId id;
	Map<String, String> preferenze;
	public boolean nuovoUser = false;

	private final MongoCollection<Document> utenti;
	
	public Utente(String n, String c, String p, String e) throws Exception{
		nome = n;
		cognome = c;
		password = p;
		email = e;
		
		username = n + "." + c;
		
		Connessione connection = new Connessione();
        this.utenti = connection.connessioneUtenti();
		
		registrazioneDB();
	}
	
	public Utente(String p, String e) throws Exception{
		password = p;
		email = e;
		
		Connessione connection = new Connessione();
        this.utenti = connection.connessioneUtenti();
        
        loginDB();
	}

	@Override
	public boolean registrazioneDB() {
		// Controlla se il database ha già questo record
		if(controlloCredenziali(this.email, this.password) != null) {
			System.out.println("Utente già registrato");
			return this.nuovoUser = false;
		}
		
		// Altrimenti lo salva nel db
        Document nuovo = new Document("nome", this.nome)
        		.append("cognome", this.cognome)
        		.append("password", this.password)
                .append("email", this.email)
                .append("username", this.username);

        utenti.insertOne(nuovo);
        
        // Recupero l'id del documento dal database
        this.id = nuovo.getObjectId("_id");
        
        return this.nuovoUser = true;
	}

	@Override
	public boolean loginDB() {
		// Controlla se il database ha già questo record
		if(controlloCredenziali(this.email, this.password) == null) {
			System.out.println("Utente non registrato");
			return false;
		}
		
		// Recupero l'id del documento dal database
		this.id = controlloCredenziali(this.email, this.password).getObjectId("_id");
		this.username = controlloCredenziali(this.email, this.password).getString("username");
		
		return true;
	}
	
	@Override
	public boolean deleteDB() {
		// Trova il record con stesso id e aggiorna il database eliminandolo
		Document eliminazione = utenti.findOneAndDelete(Filters.and(
				Filters.eq("email", this.email),
				Filters.eq("password", this.password)));
		
		return eliminazione != null;
	}

	@Override
	public boolean selezioneDB(Map<String, String> p) {
		// TODO Azione di salvataggio delle preferenze nel db
		
		List<Bson> updates = new ArrayList<>();
		p.forEach((k, v) -> updates.add(Updates.set("preferenze." + k, v)));
		
		UpdateResult update = utenti.updateOne(
			    Filters.eq("_id", this.id),
			    Updates.combine(updates)
			);
				
		return true;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public ObjectId getId() {
		return this.id;
	}

	@Override
	public Object getPreferenze() {
		Document esistente = utenti.find(Filters.eq("_id", this.id)).first();
		
		return esistente.get("preferenze");
	}

	@Override
	public Utente registrazione(String nome, String cognome, String password, String email) throws Exception{
		Utente newUser = new Utente(nome, cognome, password, email);
		registrazioneDB();
		return newUser;
	}

	@Override
	public Utente login(String password, String email) throws Exception{
		Utente user = new Utente(password, email);
		return user;
	}

	@Override
	public void logout() {
		// TODO azione di logout su app
		
	}
	
	@Override
	public void setSelezione(Map<String, String> p) {
		selezioneDB(p);
	}

	@Override
	public Document controlloCredenziali(String email, String password) {
		Document esistente = utenti.find(Filters.and(
				Filters.eq("email", email),
				Filters.eq("password", password)
				)).first();
		
		return esistente;
	}
}
