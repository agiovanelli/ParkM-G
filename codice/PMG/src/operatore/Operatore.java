package Operatore;

public class Operatore implements DatiOperatori, GestioneOperatori{
	String nomeStruttura;
	Long id;
	
	public Operatore(String n, Long i) {
		nomeStruttura = n;
		id = i;
	}
	
	@Override
	public Operatore login(String nomeStruttura, Long id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Operatore login(Operatore Operatore) {
		Operatore = new Operatore(nomeStruttura, id);
		return Operatore;
	}
	
	@Override
	public void logout(Operatore Operatore) {
		
	}
	
	@Override
	public String getNomeStruttura(Operatore Operatore) {
		return Operatore.nomeStruttura;
	}
	
	@Override
	public Long getId(Operatore Operatore) {
		return Operatore.id;
	}

}
