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
	public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
		// TODO Auto-generated method stub
		return null;
	}
}