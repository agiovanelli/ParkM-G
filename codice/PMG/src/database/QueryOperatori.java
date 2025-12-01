package database;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import operatore.Operatore;

public class QueryOperatori {

	public void getOperatoreDB(Operatore operatore) throws IOException {
		MongoCollection<Document> collection = Connessione.connessioneOperatori();
		collection.find();
	}
}
