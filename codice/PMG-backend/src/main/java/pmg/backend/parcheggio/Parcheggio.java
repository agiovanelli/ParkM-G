package pmg.backend.parcheggio;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parcheggi")
public class Parcheggio {

    @Id
    private String id;
    private String nome;
    private String area;
    private int postiTotali;
    private int postiDisponibili;

    private double latitudine;
    private double longitudine;
    
    private boolean inEmergenza; // Default: false

    public Parcheggio() {}

    public Parcheggio(String nome, String area, int postiTotali, int postiDisponibili,
                      double latitudine, double longitudine) {
        this.nome = nome;
        this.area = area;
        this.postiTotali = postiTotali;
        this.postiDisponibili = postiDisponibili;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    // Getter e Setter 
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public int getPostiTotali() { return postiTotali; }
    public void setPostiTotali(int postiTotali) { this.postiTotali = postiTotali; }
    public int getPostiDisponibili() { return postiDisponibili; }
    public void setPostiDisponibili(int postiDisponibili) { this.postiDisponibili = postiDisponibili; }
    public double getLatitudine() { return latitudine; }
    public void setLatitudine(double latitudine) { this.latitudine = latitudine; }
    public double getLongitudine() { return longitudine; }
    public void setLongitudine(double longitudine) { this.longitudine = longitudine; }
    public boolean isInEmergenza() { return inEmergenza; }
    public void setInEmergenza(boolean inEmergenza) { this.inEmergenza = inEmergenza; }
}