package pmg.backend.parcheggio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import pmg.backend.posto.Posto;

/**
 * Rappresenta il documento principale della collezione MongoDB dei parcheggi.
 *
 * Oltre alle informazioni anagrafiche e geografiche, contiene la configurazione
 * dei piani e i relativi posti come oggetti embedded, evitando una collezione
 * MongoDB separata per i singoli posti.
 */
@Document(collection = "parcheggi")
public class Parcheggio {

    /**
     * Identificativo univoco del documento.
     */
    @Id
    private String id;

    /**
     * Nome descrittivo.
     */
    private String nome;
    /**
     * Area geografica associata.
     */
    private String area;
    /**
     * Numero totale di posti configurati.
     */
    private int postiTotali;
    /**
     * Numero di posti attualmente disponibili.
     */
    private int postiDisponibili;
    /**
     * Numero complessivo di piani.
     */
    private int numPiani;
    /** Configurazione embedded dei piani e dei relativi posti. */
    private List<ConfigurazionePiano> configurazionePiani = new ArrayList<>();
    /**
     * Latitudine della posizione geografica.
     */
    private double latitudine;
    /**
     * Longitudine della posizione geografica.
     */
    private double longitudine;
    /**
     * Indica se il parcheggio è in stato di emergenza.
     */
    private boolean inEmergenza;

    /**
     * Costruttore vuoto richiesto dal framework di persistenza.
     */
    public Parcheggio() {
    }

    /**
     * Crea una nuova istanza di Parcheggio con i dati indicati.
     *
     * @param nome nome descrittivo
     * @param area area geografica di ricerca
     * @param postiTotali numero totale di posti
     * @param postiDisponibili numero di posti disponibili
     * @param latitudine latitudine
     * @param longitudine longitudine
     */
    public Parcheggio(
            String nome,
            String area,
            int postiTotali,
            int postiDisponibili,
            double latitudine,
            double longitudine) {
        this.nome = nome;
        this.area = area;
        this.postiTotali = postiTotali;
        this.postiDisponibili = postiDisponibili;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    /**
     * Restituisce tutti i posti del parcheggio aggregando i piani configurati.
     * @return lista aggregata dei posti
     */
    public List<Posto> getTuttiPosti() {
        return getConfigurazionePiani().stream()
                .flatMap(piano -> piano.getPosti().stream())
                .toList();
    }

    /**
     * Ricerca un posto all'interno della configurazione embedded del parcheggio.
     *
     * @param slotId identificativo logico del posto
     * @return posto trovato, se presente
     */
    public Optional<Posto> trovaPosto(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return Optional.empty();
        }

        return getTuttiPosti().stream()
                .filter(posto -> slotId.equals(posto.getSlotId()))
                .findFirst();
    }

    /**
     * Ricerca un posto all'interno della configurazione embedded del parcheggio.
     *
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @return posto trovato, se presente
     */
    public Optional<Posto> trovaPosto(int piano, int numero) {
        return getTuttiPosti().stream()
                .filter(posto -> posto.getPiano() == piano && posto.getNumero() == numero)
                .findFirst();
    }

    /**
     * Ricalcola numero di piani, capacità totale e disponibilità corrente.
     */
    public void ricalcolaStatistiche() {
        List<ConfigurazionePiano> piani = getConfigurazionePiani();
        this.numPiani = piani.size();
        this.postiTotali = piani.stream()
                .mapToInt(ConfigurazionePiano::getNumeroPosti)
                .sum();

        int postiGenerati = piani.stream()
                .mapToInt(piano -> piano.getPosti().size())
                .sum();

        if (postiGenerati == 0 && postiTotali > 0) {
            this.postiDisponibili = postiTotali;
        } else {
            this.postiDisponibili = (int) piani.stream()
                    .flatMap(piano -> piano.getPosti().stream())
                    .filter(Posto::isDisponibile)
                    .count();
        }
    }

    /**
     * Restituisce identificativo.
     * @return identificativo
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta identificativo.
     *
     * @param id identificativo della risorsa
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Restituisce nome.
     * @return nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta nome.
     *
     * @param nome nome descrittivo
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce area geografica.
     * @return area geografica
     */
    public String getArea() {
        return area;
    }

    /**
     * Imposta area geografica.
     *
     * @param area area geografica di ricerca
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * Restituisce numero totale di posti.
     * @return numero totale di posti
     */
    public int getPostiTotali() {
        return postiTotali;
    }

    /**
     * Imposta numero totale di posti.
     *
     * @param postiTotali numero totale di posti
     */
    public void setPostiTotali(int postiTotali) {
        this.postiTotali = postiTotali;
    }

    /**
     * Restituisce numero di posti disponibili.
     * @return numero di posti disponibili
     */
    public int getPostiDisponibili() {
        return postiDisponibili;
    }

    /**
     * Imposta numero di posti disponibili.
     *
     * @param postiDisponibili numero di posti disponibili
     */
    public void setPostiDisponibili(int postiDisponibili) {
        this.postiDisponibili = postiDisponibili;
    }

    /**
     * Restituisce numero di piani.
     * @return numero di piani
     */
    public int getNumPiani() {
        return numPiani;
    }

    /**
     * Imposta numero di piani.
     *
     * @param numPiani numero di piani
     */
    public void setNumPiani(int numPiani) {
        this.numPiani = numPiani;
    }

    /**
     * Restituisce configurazione dei piani.
     * @return configurazione dei piani
     */
    public List<ConfigurazionePiano> getConfigurazionePiani() {
        if (configurazionePiani == null) {
            configurazionePiani = new ArrayList<>();
        }
        return configurazionePiani;
    }

    /**
     * Imposta configurazione dei piani.
     *
     * @param configurazionePiani configurazione dei piani
     */
    public void setConfigurazionePiani(List<ConfigurazionePiano> configurazionePiani) {
        this.configurazionePiani = configurazionePiani == null
                ? new ArrayList<>()
                : new ArrayList<>(configurazionePiani);
    }

    /**
     * Restituisce latitudine.
     * @return latitudine
     */
    public double getLatitudine() {
        return latitudine;
    }

    /**
     * Imposta latitudine.
     *
     * @param latitudine latitudine
     */
    public void setLatitudine(double latitudine) {
        this.latitudine = latitudine;
    }

    /**
     * Restituisce longitudine.
     * @return longitudine
     */
    public double getLongitudine() {
        return longitudine;
    }

    /**
     * Imposta longitudine.
     *
     * @param longitudine longitudine
     */
    public void setLongitudine(double longitudine) {
        this.longitudine = longitudine;
    }

    /**
     * Indica se stato di emergenza.
     * @return {@code true} se la condizione è verificata, {@code false} altrimenti
     */
    public boolean isInEmergenza() {
        return inEmergenza;
    }

    /**
     * Imposta stato di emergenza.
     *
     * @param inEmergenza stato di emergenza
     */
    public void setInEmergenza(boolean inEmergenza) {
        this.inEmergenza = inEmergenza;
    }
}
