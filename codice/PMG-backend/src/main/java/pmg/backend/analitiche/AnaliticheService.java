package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

import pmg.backend.parcheggio.Parcheggio;

@Service
public interface AnaliticheService {

    /**
     * Visualizza le analitiche del parcheggio.
     * @return 
     */
    AnaliticheResponse getAnalitiche(Parcheggio parcheggio, String operatoreId);
}
