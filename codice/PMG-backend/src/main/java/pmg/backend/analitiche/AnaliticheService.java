package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

@Service
public interface AnaliticheService {
    
    Analitiche getById(String id);

    Analitiche getByOperatoreId(String operatoreId);
    
    Analitiche getByParcheggioId(String parcheggioId);

    Analitiche save(AnaliticheRequest request);
}
