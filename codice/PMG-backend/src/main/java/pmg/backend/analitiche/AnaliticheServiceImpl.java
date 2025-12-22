package pmg.backend.analitiche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnaliticheServiceImpl implements AnaliticheService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnaliticheServiceImpl.class);

    private final AnaliticheRepository repository;

    public AnaliticheServiceImpl(AnaliticheRepository repository) {
        this.repository = repository;
    }
    
	@Override
	public AnaliticheResponse getAnalitiche() {
		LOGGER.info("Recupero analitiche parcheggio");
		return new AnaliticheResponse();
	}

}
