package pmg.backend.parcheggio;

import java.util.List;

import pmg.backend.posto.PostoResponse;

/**
 * DTO utilizzato per restituire al frontend la mappa logica completa di un parcheggio.
 *
 * La risposta comprende la configurazione dei piani e tutti i posti embedded,
 * ordinati per piano e numero, necessari al frontend per generare dinamicamente
 * geometria, corsie e percorsi.
 *
 * @param id identificativo univoco del parcheggio
 * @param nome nome del parcheggio
 * @param postiTotali numero totale di posti configurati
 * @param postiDisponibili numero di posti attualmente disponibili
 * @param numPiani numero di piani del parcheggio
 * @param configurazionePiani configurazione completa dei piani e dei relativi posti
 */
public record MappaParcheggioResponse(
        String id,
        String nome,
        int postiTotali,
        int postiDisponibili,
        int numPiani,
        List<PianoResponse> configurazionePiani
) {
    /**
     * Crea una risposta completa a partire dal documento del parcheggio.
     *
     * @param parcheggio documento del parcheggio da convertire o elaborare
     * @return risposta completa della mappa logica
     */
    public static MappaParcheggioResponse from(Parcheggio parcheggio) {
        List<PianoResponse> piani = parcheggio.getConfigurazionePiani().stream()
                .sorted((a, b) -> Integer.compare(a.getPiano(), b.getPiano()))
                .map(piano -> new PianoResponse(
                        piano.getPiano(),
                        piano.getNumeroPosti(),
                        piano.getPosti().stream()
                                .sorted((a, b) -> Integer.compare(a.getNumero(), b.getNumero()))
                                .map(posto -> new PostoResponse(posto, parcheggio.getId()))
                                .toList()
                ))
                .toList();

        return new MappaParcheggioResponse(
                parcheggio.getId(),
                parcheggio.getNome(),
                parcheggio.getPostiTotali(),
                parcheggio.getPostiDisponibili(),
                parcheggio.getNumPiani(),
                piani
        );
    }

    /**
     * DTO che rappresenta un singolo piano nella risposta della mappa logica.
     *
     * @param piano numero identificativo del piano
     * @param numeroPosti capacità configurata del piano
     * @param posti elenco ordinato dei posti del piano
     */
    public record PianoResponse(
            int piano,
            int numeroPosti,
            List<PostoResponse> posti
    ) {
    }
}
