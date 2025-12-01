package operatore;

public class Operatore implements DatiOperatori, GestioneOperatori{
	String nomeStruttura;
	Long id;
	
	public Operatore(String n, Long i) {
		nomeStruttura = n;
		id = i;
	}
	
	@Override
	public Operatore login(String nomeStruttura, Long id) {
		Operatore operatore = new Operatore(nomeStruttura, id);
		return operatore;
	}
	
	@Override
	public void logout() {
		
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
