package pmg.backend.analitiche;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analitiche")
public class Analitiche {

	@Id
    private String id; //ObjectId
	
	private String parcheggio; //Sostituire con Parcheggio parcheggio
	private String operatoreId; 
	
	public Analitiche() {
	}
	
	public Analitiche(String parcheggio, String operatoreId) {
		this.parcheggio = parcheggio;
		this.operatoreId = operatoreId;
	}
	
	void getAnalitiche() {
		// get parcheggio
		// get eventi
		// get allarmi
	}
	
	void setEventi() {
		
	}
	
	void setAllarmi() {
		
	}
}
