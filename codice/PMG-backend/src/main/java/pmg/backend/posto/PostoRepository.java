package pmg.backend.posto;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostoRepository extends MongoRepository<Posto, String> {

}