package Operatore;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import Database.Connessione;

/**
 * Classe Operatore.
 */
public class Operatore implements DatiOperatori, GestioneOperatori{
	
	/** nome struttura. */
	String nomeStruttura;
	
	/** username. */
	String username;
	
	/**
	 * Instanza un nuovo operatore.
	 *
	 * @param n è il nomeStruttura
	 * @param u è lo username
	 */
	public Operatore(String n, String u) {
		nomeStruttura = n;
		username = u;
	}
	
	/**
	 * Login.
	 */
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
	
	/**
	 * Logout.
	 */
	@Override
	public void logout() {
		System.out.println("Logout eseguito correttamente");
	}
	
	/**
	 * Recupera il nome della struttura.
	 *
	 * @return il nome struttura
	 */
	@Override
	public String getNomeStruttura() {
		return nomeStruttura;
	}
	
	/**
	 * Recupera lo username.
	 *
	 * @return lo username
	 */
	@Override
	public String getUsername() {
		return username;
	}
	
	/**
	 * Controlla se esiste l'operatore nel DB.
	 *
	 * @param operatore è l'operatore che è stato creato
	 * @return true, se l'operatore esiste nel DB
	 * @throws IOException segnala che c'è stata una I/O exception.
	 */
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
