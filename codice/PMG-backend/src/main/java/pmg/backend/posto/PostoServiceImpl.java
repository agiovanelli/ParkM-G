package pmg.backend.posto;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostoServiceImpl implements PostoService {

    private final PostoRepository repository;

    public PostoServiceImpl(PostoRepository repository) {
        this.repository = repository;
    }

    @Override
    public PostoResponse creaPosto(Posto posto) {
        Posto salvato = repository.save(posto);
        return mapToResponse(salvato);
    }

    @Override
    public void eliminaPosto(String id) {
        repository.deleteById(id);
    }

    @Override
    public PostoResponse aggiornaDisponibilita(String id, boolean disponibile) {
        Posto posto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        posto.setDisponibile(disponibile);
        repository.save(posto);

        return mapToResponse(posto);
    }

    private PostoResponse mapToResponse(Posto posto) {
        return new PostoResponse(posto);
    }

	@Override
	public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PostoResponse aggiornaDisabiliato(String id, boolean disabilitato) {
		Posto posto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Posto non trovato"));

        posto.setDisabilitato(disabilitato);
        posto.setDisponibile(false);
        repository.save(posto);

        return mapToResponse(posto);
	}
}