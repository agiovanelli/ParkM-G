package operatore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;

import database.Connessione;


class OperatoreTest {

    /**
     * Helper per creare un Operatore con i campi necessari.
     *
     * 🔧 ADATTA QUI se il tuo costruttore ha una firma diversa.
     */
    private Operatore creaOperatore(String nomeStruttura, String username) throws IOException {
        // Esempio base: costruttore (nomeStruttura, username)
        return new Operatore(nomeStruttura, username);
        // Se in realtà hai qualcosa tipo:
        // return new Operatore(nomeStruttura, username, password, email, ...);
        // modifica qui di conseguenza.
    }

    /**
     * Genera un nome struttura univoco per evitare collisioni tra test.
     */
    private String nomeStrutturaUnico() {
        return "StrutturaTest_" + System.nanoTime();
    }

    /**
     * Genera uno username univoco per evitare collisioni tra test.
     */
    private String usernameUnico() {
        return "operatoreTest_" + System.nanoTime();
    }

    @Test
    void esisteOperatoreRitornaTrueQuandoDocumentoPresente() throws IOException {
        MongoCollection<Document> collection = Connessione.connessioneOperatori();

        String nomeStruttura = nomeStrutturaUnico();
        String username = usernameUnico();

        // Pulizia preventiva
        collection.deleteMany(new Document("nomeStruttura", nomeStruttura)
                                      .append("username", username));

        // Inserisco il documento che il metodo dovrà trovare
        Document doc = new Document()
                .append("nomeStruttura", nomeStruttura)
                .append("username", username);
        collection.insertOne(doc);

        // Operatore passato come parametro (contiene i dati da cercare)
        Operatore operatoreParametro = creaOperatore(nomeStruttura, username);

        // L'istanza su cui chiamo il metodo (il "service") non è importante:
        // esisteOperatore usa solo i getter dell'argomento.
        Operatore service = creaOperatore("qualcosa", "qualcosa");

        boolean esiste = service.esisteOperatore(operatoreParametro);

        assertTrue(esiste,
                "esisteOperatore deve restituire true quando esiste un documento con stessi nomeStruttura e username");
    }

    @Test
    void esisteOperatoreRitornaFalseQuandoDocumentoAssente() throws IOException {
        MongoCollection<Document> collection = Connessione.connessioneOperatori();

        String nomeStruttura = nomeStrutturaUnico();
        String username = usernameUnico();

        // Mi assicuro che NON ci siano documenti con questi valori
        collection.deleteMany(new Document("nomeStruttura", nomeStruttura)
                                      .append("username", username));

        Operatore operatoreParametro = creaOperatore(nomeStruttura, username);
        Operatore service = creaOperatore("qualcosa", "qualcosa");

        boolean esiste = service.esisteOperatore(operatoreParametro);

        assertFalse(esiste,
                "esisteOperatore deve restituire false quando non esiste alcun documento con quei campi");
    }
}
