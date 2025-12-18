package pmg.backend.parcheggio;

import java.util.List;

public interface ParcheggioService {
    List<ParcheggioResponse> cercaPerArea(String area);
}