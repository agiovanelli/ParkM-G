package pmg.backend.posto;

import java.util.List;

public interface PostoService {

    List<PostoResponse> getPostiByParcheggio(String parcheggioId);
    
    void generaPosti(String parcheggioId);
}