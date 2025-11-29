package database;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Connessione {

	public static void main( String[] args ) {
        String uri = "mongodb+srv://admin:admin@pmg.pnsnxvt.mongodb.net/?appName=PMG";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("PMG");
            MongoCollection<Document> collection = database.getCollection("operatori");
            Document doc = collection.find().first();
            if (doc != null) {
                System.out.println(doc.toJson());
            } else {
                System.out.println("No matching documents found.");
            }
        }
    }
}
