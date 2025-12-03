package Operatore;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import Database.Connessione;

public class Operatore implements DatiOperatori, GestioneOperatori{
	String nomeStruttura;
	String username;
	
	public Operatore(String n, String u) {
		nomeStruttura = n;
		username = u;
	}
	
	@Override
	public void login() {
        try {
            boolean esiste = esisteOperatore(this);

            if (esiste) {
                System.out.println("Login effettuato");
            } else {
                System.out.println("Utente non registrato");
            }
        } catch (IOException e) {
            System.out.println("Errore di connessione al database: " + e.getMessage());
            e.printStackTrace();
        }
    }
	
	@Override
	public void logout() {
		
	}
	
	@Override
	public String getNomeStruttura() {
		return nomeStruttura;
	}
	
	@Override
	public String getUsername() {
		return username;
	}
	
	@Override
	public boolean esisteOperatore(Operatore operatore) throws IOException {
	    MongoCollection<Document> collection = Connessione.connessioneOperatori();

	    Document filtro = new Document()
	            .append("nomeStruttura", operatore.getNomeStruttura())
	            .append("username", operatore.getUsername());

	    long count = collection.countDocuments(filtro);
	    return count > 0;
	}

}
