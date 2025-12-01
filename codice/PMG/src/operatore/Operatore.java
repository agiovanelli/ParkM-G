package operatore;

import java.io.IOException;

import database.QueryOperatori;

public class Operatore implements DatiOperatori, GestioneOperatori{
	String nomeStruttura;
	String username;
	
	public Operatore(String n, String u) {
		nomeStruttura = n;
		username = u;
	}
	
	@Override
	public void login() {
        try {
            QueryOperatori qo = new QueryOperatori();
            boolean esiste = qo.esisteOperatore(this);

            if (esiste) {
                System.out.println("Login effettuato");
            } else {
                System.out.println("Utente non registrato");
            }
        } catch (IOException e) {
            System.out.println("Errore di connessione al database: " + e.getMessage());
            e.printStackTrace();
        }
    }
	
	@Override
	public void logout() {
		
	}
	
	@Override
	public String getNomeStruttura() {
		return nomeStruttura;
	}
	
	@Override
	public String getUsername() {
		return username;
	}

}
