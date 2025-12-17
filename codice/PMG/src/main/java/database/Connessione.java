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
	
	private static MongoClient mongoClient;
	
	private Connessione() {
		
	}
	
	private static String loadMongoUri() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("conf.properties")) {
            props.load(fis);
        }
        String uri = props.getProperty("mongo.uri");
        
        
        if (uri == null || uri.trim().isEmpty()) { 
            throw new IllegalStateException("Chiave mongo.uri mancante in conf.properties");
        }
        return uri;
	}
	
	private static MongoClient getMongoClient() throws IOException {
        // Se il client è nullo o chiuso, lo apriamo
        if (mongoClient == null) {
            String uri = loadMongoUri();
            mongoClient = MongoClients.create(uri);
        }
        return mongoClient;
    }
	public static void closeClient() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
	
	public static MongoCollection<Document> connessioneUtenti() throws IOException{
        MongoClient client = getMongoClient(); // Ottieni il client singleton
        MongoDatabase database = client.getDatabase("PMG");
        return database.getCollection("utenti"); // Ritorna la collezione senza chiudere il client
	}
    
	public static MongoCollection<Document> connessioneOperatori() throws IOException{
        MongoClient client = getMongoClient(); // Ottieni il client singleton
        MongoDatabase database = client.getDatabase("PMG");
        return database.getCollection("operatori"); // Ritorna la collezione senza chiudere il client
	}
}