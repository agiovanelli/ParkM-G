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

    private String tipo;        // "EVENTO" | "ALLARME"
    private String descrizione; // testo evento/segnalazione
    private String timestamp;   // ISO string o epoch

    public Analitiche() {}

    public Analitiche(
            String parcheggioId,
            String nomeParcheggio,
            String operatoreId,
            String tipo,
            String descrizione,
            String timestamp) {

        this.parcheggioId = parcheggioId;
        this.nomeParcheggio = nomeParcheggio;
        this.operatoreId = operatoreId;
        this.tipo = tipo;
        this.descrizione = descrizione;
        this.timestamp = timestamp;
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

    public String getTipo() {
        return tipo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
