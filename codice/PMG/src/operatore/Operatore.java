package operatore;

import java.io.IOException;

import org.bson.Document;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import com.mongodb.client.MongoCollection;

import database.Connessione;

/**
 * Classe Operatore.
 */
public class Operatore implements DatiOperatori, GestioneOperatori{
	private static final Logger LOGGER = LoggerFactory.getLogger(Operatore.class);
	
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
            	LOGGER.info("Login effettuato per l'operatore: {}", username);
            } else {
            	LOGGER.warn("Tentativo di login fallito. Operatore non registrato: {}", username);
            }
        } catch (IOException e) {
        	LOGGER.error("Errore di connessione al database durante il login", e);
            e.printStackTrace();
        }
    }
	
	/**
	 * Logout.
	 */
	@Override
	public void logout() {
		LOGGER.info("Logout eseguito correttamente per l'operatore: {}", username);
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
