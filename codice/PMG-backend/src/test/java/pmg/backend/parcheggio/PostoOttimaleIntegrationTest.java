package pmg.backend.parcheggio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.posto.Posto;
import pmg.backend.posto.PostoRepository;

@SpringBootTest
class PostoOttimaleIntegrationTest {

    @Autowired
    private ParcheggioService parcheggioService;

    @Autowired
    private PostoRepository postoRepository;

    @BeforeEach
    void cleanDb() {
        postoRepository.deleteAll();
    }

	    @Test
	    void assegnaPostoOttimale_disabile_scegliePostoRiservato() {
	        String parcheggioId = "P1";

	        Posto normale = new Posto();
	        normale.setParcheggioId(parcheggioId);
	        normale.setDisponibile(true);
	        normale.setDisabilitato(false);
	        normale.setRiservatoDisabili(false);
	        normale.setRiservatoIncinta(false);
	        normale.setDistanzaUscita(1);

	        Posto disabili = new Posto();
	        disabili.setParcheggioId(parcheggioId);
	        disabili.setDisponibile(true);
	        disabili.setDisabilitato(false);
	        disabili.setRiservatoDisabili(true);
	        disabili.setRiservatoIncinta(false);
	        disabili.setDistanzaUscita(5);

	        postoRepository.saveAll(List.of(normale, disabili));

	        Map<String, String> preferenze = Map.of(
	            "disabile", "Si",
	            "distanza", "2"
	        );

	        Posto risultato = parcheggioService.assegnaPostoOttimale(parcheggioId, preferenze);

	        assertNotNull(risultato);
	        assertTrue(risultato.isRiservatoDisabili());
	    }
	    
	    @Test
	    void assegnaPostoOttimale_nonDisabile_evitaPostoDisabili() {
	        String parcheggioId = "P1";

	        Posto disabili = new Posto();
	        disabili.setParcheggioId(parcheggioId);
	        disabili.setDisponibile(true);
	        disabili.setRiservatoDisabili(true);

	        Posto normale = new Posto();
	        normale.setParcheggioId(parcheggioId);
	        normale.setDisponibile(true);
	        normale.setRiservatoDisabili(false);

	        postoRepository.saveAll(List.of(disabili, normale));

	        Map<String, String> pref = Map.of("disabile", "No");

	        Posto result = parcheggioService.assegnaPostoOttimale(parcheggioId, pref);

	        assertFalse(result.isRiservatoDisabili());
	    }
	    
	    @Test
	    void assegnaPostoOttimale_incinta_scegliePostoRiservato() {
	        String parcheggioId = "P1";

	        Posto normale = new Posto();
	        normale.setParcheggioId(parcheggioId);
	        normale.setDisponibile(true);

	        Posto incinta = new Posto();
	        incinta.setParcheggioId(parcheggioId);
	        incinta.setDisponibile(true);
	        incinta.setRiservatoIncinta(true);

	        postoRepository.saveAll(List.of(normale, incinta));

	        Map<String, String> pref = Map.of("donnaIncinta", "Si");

	        Posto result = parcheggioService.assegnaPostoOttimale(parcheggioId, pref);

	        assertTrue(result.isRiservatoIncinta());
	    }
	    
	    @Test
	    void assegnaPostoOttimale_sceglieDistanzaMigliore() {
	        String parcheggioId = "P1";

	        Posto p1 = new Posto();
	        p1.setParcheggioId(parcheggioId);
	        p1.setDisponibile(true);
	        p1.setDistanzaUscita(1);

	        Posto p2 = new Posto();
	        p2.setParcheggioId(parcheggioId);
	        p2.setDisponibile(true);
	        p2.setDistanzaUscita(5);

	        postoRepository.saveAll(List.of(p1, p2));

	        Map<String, String> pref = Map.of("distanza", "2");

	        Posto result = parcheggioService.assegnaPostoOttimale(parcheggioId, pref);

	        assertEquals(1, result.getDistanzaUscita());
	    }
	    
	    @Test
	    void assegnaPostoOttimale_nessunPosto_returnNull() {
	        String parcheggioId = "P1";

	        Map<String, String> pref = Map.of();

	        Posto result = parcheggioService.assegnaPostoOttimale(parcheggioId, pref);

	        assertNull(result);
	    }
}
