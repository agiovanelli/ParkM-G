package pmg.backend.analitiche;

import org.springframework.data.annotation.Id;

public class AnaliticheResponse {

    private String id;
	private String parcheggio; //Sostituire con Parcheggio parcheggio
	private String operatoreId; 
	
	public AnaliticheResponse() {
	}
	
	public AnaliticheResponse(String parcheggio, String operatoreId) {
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
