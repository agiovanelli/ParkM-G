package pmg.backend.analitiche;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analitiche")
public class Analitiche {

    @Id
    private String id;

    private String parcheggioId;
    private String nomeParcheggio;
    private String operatoreId;

    public Analitiche() {}

    public Analitiche(
            String parcheggioId,
            String nomeParcheggio,
            String operatoreId) {

        this.parcheggioId = parcheggioId;
        this.nomeParcheggio = nomeParcheggio;
        this.operatoreId = operatoreId;
    }

    public String getId() {
        return id;
    }

    public String getParcheggioId() {
        return parcheggioId;
    }

    public String getNomeParcheggio() {
        return nomeParcheggio;
    }

    public String getOperatoreId() {
        return operatoreId;
    }
}
