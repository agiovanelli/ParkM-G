package database;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;


class ConnessioneTest {

    @Test
    void connessioneUtentiRestituisceCollezioneNonNullEConNomeCorretto() throws IOException {
        MongoCollection<Document> utenti = Connessione.connessioneUtenti();

        assertNotNull(utenti, "connessioneUtenti() non deve restituire null");
        assertEquals("utenti",
                utenti.getNamespace().getCollectionName(),
                "Il nome della collezione deve essere 'utenti'");

        String nomeDb = utenti.getNamespace().getDatabaseName();
        assertEquals("PMG", nomeDb, "Il database deve chiamarsi 'PMG'");
    }

    @Test
    void connessioneOperatoriRestituisceCollezioneNonNullEConNomeCorretto() throws IOException {
        MongoCollection<Document> operatori = Connessione.connessioneOperatori();

        assertNotNull(operatori, "connessioneOperatori() non deve restituire null");
        assertEquals("operatori",
                operatori.getNamespace().getCollectionName(),
                "Il nome della collezione deve essere 'operatori'");

        String nomeDb = operatori.getNamespace().getDatabaseName();
        assertEquals("PMG", nomeDb, "Il database deve chiamarsi 'PMG'");
    }

    @Test
    void connessioneUtentiEOperatoriUsanoLoStessoDatabase() throws IOException {
        MongoCollection<Document> utenti = Connessione.connessioneUtenti();
        MongoCollection<Document> operatori = Connessione.connessioneOperatori();

        assertNotNull(utenti);
        assertNotNull(operatori);

        String dbUtenti = utenti.getNamespace().getDatabaseName();
        String dbOperatori = operatori.getNamespace().getDatabaseName();

        assertEquals(dbUtenti, dbOperatori,
                "Le collezioni 'utenti' e 'operatori' devono puntare allo stesso database");
    }
}
