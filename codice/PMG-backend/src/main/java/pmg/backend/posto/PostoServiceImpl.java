package pmg.backend.posto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostoServiceImpl implements PostoService {

	@Autowired
    private final PostoRepository postoRepository;

    public PostoServiceImpl(PostoRepository postoRepository) {
        this.postoRepository = postoRepository;
    }

	@Override
	public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
		// TODO Auto-generated method stub
		return null;
	}
	
    public void generaPosti(String parcheggioId) {

        List<Posto> posti = new ArrayList<>();

        for (int piano = 1; piano <= 3; piano++) {
            for (int numero = 1; numero <= 18; numero++) {

                Posto p = new Posto();
                p.setPiano(piano);
                p.setNumero(numero);
                	
                if(numero < 8)
                	p.setDistanzaUscita(1);
                else if(numero > 7 && numero < 15)
                	p.setDistanzaUscita(4);
                else if(numero > 14 && numero < 17)
                	p.setDistanzaUscita(2);
                else if(numero < 16)
                	p.setDistanzaUscita(3);

                p.setDisponibile(true);
                p.setDisabilitato(false);

                // Logica opzionale
                p.setRiservatoDisabili(numero >= 17);
                p.setRiservatoIncinta(numero >= 15 && numero <= 16);

                p.setParcheggioId(parcheggioId);

                posti.add(p);
            }
        }

        postoRepository.saveAll(posti);
    }
}