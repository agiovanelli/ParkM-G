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
	
	public Connessione() {
		
	}
	
	private static String loadMongoUri() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("conf.properties")) {
            props.load(fis);
        }
        String uri = props.getProperty("mongo.uri");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Chiave mongo.uri mancante in conf.properties");
        }
        return uri;
    }
	
	public static MongoCollection<Document> connessioneUtenti() throws IOException{
		String uri = loadMongoUri();
		MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("PMG");
        MongoCollection<Document> collection = database.getCollection("utenti");
        return collection;
	}
	
	public static MongoCollection<Document> connessioneOperatori() throws IOException{
		String uri = loadMongoUri();
		MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("PMG");
        MongoCollection<Document> collection = database.getCollection("operatori");
        return collection;
	}
}
