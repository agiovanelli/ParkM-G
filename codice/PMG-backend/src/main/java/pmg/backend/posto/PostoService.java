package pmg.backend.posto;

import java.util.List;

public interface PostoService {

    List<PostoResponse> getPostiByParcheggio(String parcheggioId);

    List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano);

    void generaPosti(String parcheggioId);

    PostoResponse aggiornaDisponibilita(String parcheggioId, int piano, int numero, boolean disponibile);

    PostoResponse aggiornaDisabilitato(String parcheggioId, int piano, int numero, boolean disabilitato);
}