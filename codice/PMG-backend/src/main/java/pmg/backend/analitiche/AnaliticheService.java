package pmg.backend.analitiche;

import org.springframework.stereotype.Service;

import pmg.backend.parcheggio.Parcheggio;

@Service
public interface AnaliticheService {
    
    Analitiche getById(String id);

    Analitiche getByOperatoreId(String operatoreId);

    Analitiche save(Analitiche analitiche);
}
