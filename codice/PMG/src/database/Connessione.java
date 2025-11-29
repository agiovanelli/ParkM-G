package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Connessione {
	
	private static String loadMongoUri() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        }
        String uri = props.getProperty("mongo.uri");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Chiave mongo.uri mancante in config.properties");
        }
        return uri;
    }

	public static void main( String[] args ) throws IOException {
		String uri = loadMongoUri();
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
