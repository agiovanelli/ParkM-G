package pmg.backend.analitiche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pmg.backend.parcheggio.Parcheggio;

@Service
public class AnaliticheServiceImpl implements AnaliticheService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnaliticheServiceImpl.class);

    private final AnaliticheRepository repository;

    public AnaliticheServiceImpl(AnaliticheRepository repository) {
        this.repository = repository;
    }
    
	@Override
	public AnaliticheResponse getAnalitiche(Parcheggio parcheggio, String operatoreId) {
		LOGGER.info("Recupero analitiche parcheggio");
		return new AnaliticheResponse();
	}

}
