package operatore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;

import database.Connessione;

/**
 * Test JUnit 5 per l'interfaccia DatiOperatori,
 * verificando il comportamento dell'implementazione Operatore
 * sul metodo esisteOperatore().
 *
 * ATTENZIONE:
 * Questi test usano il database reale configurato in Connessione.connessioneOperatori().
 * È consigliato usare un DB di TEST.
 */
class DatiOperatoriTest {

    private Operatore creaOperatore(String nomeStruttura, String username) throws IOException {
        // 🔧 ADATTA QUESTO COSTRUTTORE alla tua implementazione reale
        return new Operatore(nomeStruttura, username);
    }

    private String nomeStrutturaUnico() {
        return "StrutturaTest_" + System.nanoTime();
    }

    private String usernameUnico() {
        return "operatoreTest_" + System.nanoTime();
    }

    @Test
    void operatoreImplementaDatiOperatori() throws IOException {
        DatiOperatori op = creaOperatore("StrutturaX", "userX");
        assertTrue(op instanceof DatiOperatori,
                "Operatore deve implementare l'interfaccia DatiOperatori");
    }

    @Test
    void esisteOperatoreTrueSePresenteNelDb() throws IOException {
        MongoCollection<Document> collection = Connessione.connessioneOperatori();

        String nomeStruttura = nomeStrutturaUnico();
        String username = usernameUnico();

        // Pulizia eventuali precedenti
        collection.deleteMany(new Document("nomeStruttura", nomeStruttura)
                                      .append("username", username));

        // Inserisco il documento che dovrà essere trovato
        Document doc = new Document()
                .append("nomeStruttura", nomeStruttura)
                .append("username", username);
        collection.insertOne(doc);

        // Parametro da passare al metodo
        Operatore operatoreParametro = creaOperatore(nomeStruttura, username);

        // Implementazione vista tramite interfaccia
        DatiOperatori gestione = creaOperatore("dummy", "dummy");

        boolean esiste = gestione.esisteOperatore(operatoreParametro);

        assertTrue(esiste,
                "Da DatiOperatori, esisteOperatore deve restituire true se il documento esiste nel DB");
    }

    @Test
    void esisteOperatoreFalseSeAssenteNelDb() throws IOException {
        MongoCollection<Document> collection = Connessione.connessioneOperatori();

        String nomeStruttura = nomeStrutturaUnico();
        String username = usernameUnico();

        // Mi assicuro che non ci siano documenti con questi valori
        collection.deleteMany(new Document("nomeStruttura", nomeStruttura)
                                      .append("username", username));

        Operatore operatoreParametro = creaOperatore(nomeStruttura, username);
        DatiOperatori gestione = creaOperatore("dummy", "dummy");

        boolean esiste = gestione.esisteOperatore(operatoreParametro);

        assertFalse(esiste,
                "Da DatiOperatori, esisteOperatore deve restituire false se il documento non esiste nel DB");
    }
}
