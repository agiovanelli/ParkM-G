package pmg.backend.posto;

import java.util.List;

public interface PostoService {

    List<PostoResponse> getPostiByParcheggio(String parcheggioId);

    PostoResponse creaPosto(Posto posto);

    void eliminaPosto(String id);

    PostoResponse aggiornaDisponibilita(String id, boolean disponibile);
}