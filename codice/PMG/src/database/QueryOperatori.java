package database;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import Operatore.Operatore;

public class QueryOperatori {
	
	public QueryOperatori() {
		
	}

	public boolean esisteOperatore(Operatore operatore) throws IOException {
	    MongoCollection<Document> collection = Connessione.connessioneOperatori();

	    Document filtro = new Document()
	            .append("nomeStruttura", operatore.getNomeStruttura())
	            .append("username", operatore.getUsername());

	    long count = collection.countDocuments(filtro);
	    return count > 0;
	}

}
