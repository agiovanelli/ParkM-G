package pmg.backend.posto;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostoServiceImpl implements PostoService {

    private final PostoRepository postoRepository;

    public PostoServiceImpl(PostoRepository postoRepository) {
        this.postoRepository = postoRepository;
    }

	@Override
	public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
		// TODO Auto-generated method stub
		return null;
	}
}