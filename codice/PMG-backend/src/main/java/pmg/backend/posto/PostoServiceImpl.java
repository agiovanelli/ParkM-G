package pmg.backend.posto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmg.backend.parcheggio.ConfigurazionePiano;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

/**
 * Implementazione del servizio per la gestione dei posti embedded.
 *
 * Le modifiche vengono applicate direttamente al documento {@link pmg.backend.parcheggio.Parcheggio},
 * che viene poi salvato nuovamente tramite il repository dei parcheggi.
 */
@Service
public class PostoServiceImpl implements PostoService {

    /**
     * Repository per l'accesso ai parcheggi.
     */
    private final ParcheggioRepository parcheggioRepository;

    /**
     * Crea una nuova istanza di PostoServiceImpl con i dati indicati.
     *
     * @param parcheggioRepository parcheggio repository
     */
    public PostoServiceImpl(ParcheggioRepository parcheggioRepository) {
        this.parcheggioRepository = parcheggioRepository;
    }

    /**
     * Restituisce posti by parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return posti by parcheggio
     */
    @Override
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId) {
        return getPostiByParcheggio(parcheggioId, null);
    }

    /**
     * Restituisce posti by parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @return posti by parcheggio
     */
    @Override
    @Transactional
    public List<PostoResponse> getPostiByParcheggio(String parcheggioId, Integer piano) {
        generaPosti(parcheggioId);
        Parcheggio parcheggio = getParcheggio(parcheggioId);

        return parcheggio.getTuttiPosti().stream()
                .filter(posto -> piano == null || posto.getPiano() == piano)
                .sorted(Comparator.comparingInt(Posto::getPiano)
                        .thenComparingInt(Posto::getNumero))
                .map(posto -> new PostoResponse(posto, parcheggioId))
                .toList();
    }

    /**
     * Genera o completa i posti embedded sulla base della configurazione dei piani.
     *
     * @param parcheggioId identificativo del parcheggio
     */
    @Override
    @Transactional
    public void generaPosti(String parcheggioId) {
        Parcheggio parcheggio = getParcheggio(parcheggioId);

        if (parcheggio.getConfigurazionePiani().isEmpty()) {
            throw new IllegalStateException(
                    "Configurazione piani assente per il parcheggio " + parcheggioId);
        }

        for (ConfigurazionePiano configurazione : parcheggio.getConfigurazionePiani()) {
            if (configurazione.getPiano() <= 0) {
                throw new IllegalStateException("Numero piano non valido");
            }
            if (configurazione.getNumeroPosti() < 0) {
                throw new IllegalStateException(
                        "Numero posti non valido per il piano " + configurazione.getPiano());
            }

            Map<Integer, Posto> esistentiPerNumero = new HashMap<>();
            for (Posto posto : configurazione.getPosti()) {
                esistentiPerNumero.put(posto.getNumero(), posto);
            }

            List<Posto> rigenerati = new ArrayList<>();
            for (int numero = 1; numero <= configurazione.getNumeroPosti(); numero++) {
                Posto posto = esistentiPerNumero.get(numero);
                if (posto == null) {
                    posto = creaPosto(
                            configurazione.getPiano(),
                            numero,
                            configurazione.getNumeroPosti());
                } else {
                    normalizzaPosto(
                            posto,
                            configurazione.getPiano(),
                            numero,
                            configurazione.getNumeroPosti());
                }
                rigenerati.add(posto);
            }

            configurazione.setPosti(rigenerati);
        }

        parcheggio.ricalcolaStatistiche();
        parcheggioRepository.save(parcheggio);
    }

    /**
     * Aggiorna la disponibilità di un posto embedded.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disponibile nuovo valore della disponibilità
     * @return posto aggiornato
     */
    @Override
    @Transactional
    public PostoResponse aggiornaDisponibilita(
            String parcheggioId,
            int piano,
            int numero,
            boolean disponibile) {
        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        Posto posto = parcheggio.trovaPosto(piano, numero)
                .orElseThrow(() -> new IllegalArgumentException("Posto non trovato"));

        posto.setStato(disponibile ? StatoPosto.LIBERO : StatoPosto.PRENOTATO);
        salvaConStatistiche(parcheggio);
        return new PostoResponse(posto, parcheggioId);
    }

    /**
     * Aggiorna il campo legacy di disabilitazione del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param disabilitato nuovo valore legacy di disabilitazione
     * @return posto aggiornato
     */
    @Override
    @Transactional
    public PostoResponse aggiornaDisabilitato(
            String parcheggioId,
            int piano,
            int numero,
            boolean disabilitato) {
        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        Posto posto = parcheggio.trovaPosto(piano, numero)
                .orElseThrow(() -> new IllegalArgumentException("Posto non trovato"));

        validaMessaFuoriServizio(posto, disabilitato);
        posto.setFuoriServizio(disabilitato);
        salvaConStatistiche(parcheggio);
        return new PostoResponse(posto, parcheggioId);
    }

    /**
     * Aggiorna la messa fuori servizio del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param fuoriServizio nuovo valore della messa fuori servizio
     * @return posto aggiornato
     */
    @Override
    @Transactional
    public PostoResponse aggiornaFuoriServizio(
            String parcheggioId,
            String slotId,
            boolean fuoriServizio) {
        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        Posto posto = trovaPosto(parcheggio, slotId);

        validaMessaFuoriServizio(posto, fuoriServizio);
        posto.setFuoriServizio(fuoriServizio);
        salvaConStatistiche(parcheggio);
        return new PostoResponse(posto, parcheggioId);
    }

    /**
     * Aggiorna lo stato operativo del posto.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @param stato nuovo stato operativo
     * @return posto aggiornato
     */
    @Override
    @Transactional
    public PostoResponse aggiornaStato(
            String parcheggioId,
            String slotId,
            StatoPosto stato) {
        if (stato == null) {
            throw new IllegalArgumentException("Stato posto obbligatorio");
        }

        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        Posto posto = trovaPosto(parcheggio, slotId);

        if (posto.isFuoriServizio() && stato != StatoPosto.LIBERO) {
            throw new IllegalStateException("Il posto è fuori servizio");
        }

        posto.setStato(stato);
        salvaConStatistiche(parcheggio);
        return new PostoResponse(posto, parcheggioId);
    }

    /**
     * Seleziona il posto ottimale, lo marca come prenotato e salva il parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param preferenze preferenze dell'utente
     * @return posto selezionato e marcato come prenotato
     */
    @Override
    @Transactional
    public Posto prenotaPostoOttimale(
            String parcheggioId,
            Map<String, String> preferenze) {
        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        Posto posto = selezionaPostoOttimale(preferenze, parcheggio.getTuttiPosti());

        if (posto == null) {
            return null;
        }

        posto.setStato(StatoPosto.PRENOTATO);
        salvaConStatistiche(parcheggio);
        return posto;
    }

    /**
     * Ricerca un posto all'interno della configurazione embedded del parcheggio.
     *
     * @param parcheggioId identificativo del parcheggio
     * @param slotId identificativo logico del posto
     * @return posto trovato, se presente
     */
    @Override
    @Transactional
    public Posto trovaPosto(String parcheggioId, String slotId) {
        Parcheggio parcheggio = preparaParcheggio(parcheggioId);
        return trovaPosto(parcheggio, slotId);
    }

    /**
     * Carica il parcheggio e genera gli eventuali posti mancanti.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return parcheggio pronto all'uso
     */
    private Parcheggio preparaParcheggio(String parcheggioId) {
        generaPosti(parcheggioId);
        return getParcheggio(parcheggioId);
    }

    /**
     * Recupera il parcheggio o solleva un errore se non esiste.
     *
     * @param parcheggioId identificativo del parcheggio
     * @return parcheggio trovato
     */
    private Parcheggio getParcheggio(String parcheggioId) {
        return parcheggioRepository.findById(parcheggioId)
                .orElseThrow(() -> new IllegalArgumentException("Parcheggio non trovato"));
    }

    /**
     * Ricerca un posto all'interno della configurazione embedded del parcheggio.
     *
     * @param parcheggio documento del parcheggio da convertire o elaborare
     * @param slotId identificativo logico del posto
     * @return posto trovato, se presente
     */
    private Posto trovaPosto(Parcheggio parcheggio, String slotId) {
        return parcheggio.trovaPosto(slotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Posto non trovato: " + slotId));
    }

    /**
     * Ricalcola le statistiche e salva il parcheggio.
     *
     * @param parcheggio documento del parcheggio da convertire o elaborare
     */
    private void salvaConStatistiche(Parcheggio parcheggio) {
        parcheggio.ricalcolaStatistiche();
        parcheggioRepository.save(parcheggio);
    }

    /**
     * Verifica che il posto possa essere messo fuori servizio.
     *
     * @param posto posto da convertire o aggiornare
     * @param fuoriServizio nuovo valore della messa fuori servizio
     */
    private void validaMessaFuoriServizio(Posto posto, boolean fuoriServizio) {
        if (fuoriServizio && posto.getStato() != StatoPosto.LIBERO) {
            throw new IllegalStateException(
                    "È possibile mettere fuori servizio solo un posto libero");
        }
    }

    /**
     * Crea un nuovo posto con identificativi e proprietà iniziali coerenti.
     *
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param numeroPostiPiano numero totale di posti del piano
     * @return nuovo posto generato
     */
    private Posto creaPosto(int piano, int numero, int numeroPostiPiano) {
        String slotId = formatSlotId(piano, numero);
        return new Posto(
                slotId,
                numero,
                "P" + piano + "-" + String.format("%02d", numero),
                piano,
                determinaTipo(numero, numeroPostiPiano),
                StatoPosto.LIBERO,
                false,
                calcolaDistanzaUscita(numero, numeroPostiPiano));
    }

    /**
     * Normalizza i dati di un posto già esistente rispetto alla configurazione corrente.
     *
     * @param posto posto da convertire o aggiornare
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @param numeroPostiPiano numero totale di posti del piano
     */
    private void normalizzaPosto(
            Posto posto,
            int piano,
            int numero,
            int numeroPostiPiano) {
        posto.setPiano(piano);
        posto.setNumero(numero);
        posto.setSlotId(formatSlotId(piano, numero));

        if (posto.getNome() == null || posto.getNome().isBlank()) {
            posto.setNome("P" + piano + "-" + String.format("%02d", numero));
        }
        if (posto.getTipo() == null) {
            posto.setTipo(determinaTipo(numero, numeroPostiPiano));
        }
        if (posto.getStato() == null) {
            posto.setStato(StatoPosto.LIBERO);
        }
        if (posto.getDistanzaUscita() <= 0) {
            posto.setDistanzaUscita(
                    calcolaDistanzaUscita(numero, numeroPostiPiano));
        }
    }

    /**
     * Costruisce l'identificativo logico del posto.
     *
     * @param piano piano da filtrare o modificare
     * @param numero numero progressivo del posto
     * @return identificativo nel formato piano-numero
     */
    private String formatSlotId(int piano, int numero) {
        return piano + "-" + String.format("%02d", numero);
    }

    /**
     * Determina la categoria del posto in base alla sua posizione nel piano.
     *
     * @param numero numero progressivo del posto
     * @param numeroPostiPiano numero totale di posti del piano
     * @return categoria assegnata al posto
     */
    private TipoPosto determinaTipo(int numero, int numeroPostiPiano) {
        if (numeroPostiPiano >= 2 && numero > numeroPostiPiano - 2) {
            return TipoPosto.DISABILI;
        }
        if (numeroPostiPiano >= 4 && numero > numeroPostiPiano - 4) {
            return TipoPosto.INCINTA;
        }
        return TipoPosto.NORMALE;
    }

    /**
     * Calcola l'indice logico di distanza del posto dall'uscita.
     *
     * @param numero numero progressivo del posto
     * @param numeroPostiPiano numero totale di posti del piano
     * @return indice logico di distanza
     */
    private int calcolaDistanzaUscita(int numero, int numeroPostiPiano) {
        if (numeroPostiPiano <= 1) {
            return 1;
        }
        int fascia = 1 + ((numero - 1) * 4 / numeroPostiPiano);
        return Math.max(1, Math.min(4, fascia));
    }

    /**
     * Valuta i posti disponibili e restituisce quello con il punteggio migliore.
     *
     * @param preferenzeUtente preferenze utente
     * @param posti elenco dei posti del piano
     * @return posto con il punteggio migliore o {@code null}
     */
    private Posto selezionaPostoOttimale(
            Map<String, String> preferenzeUtente,
            List<Posto> posti) {
        Map<String, String> preferenze = preferenzeUtente == null
                ? Map.of()
                : preferenzeUtente;

        boolean disabile = "Si".equalsIgnoreCase(preferenze.get("disabile"));
        boolean incinta = "Si".equalsIgnoreCase(preferenze.get("donnaIncinta"));
        int distanzaPreferita = parseIntOrDefault(preferenze.get("distanza"), 1);

        TipoPosto tipoRichiesto = disabile
                ? TipoPosto.DISABILI
                : incinta ? TipoPosto.INCINTA : TipoPosto.NORMALE;

        Posto migliore = null;
        int punteggioMigliore = Integer.MIN_VALUE;

        for (Posto posto : posti) {
            if (posto == null || !posto.isDisponibile()) {
                continue;
            }
            if (posto.getTipo() != tipoRichiesto) {
                continue;
            }

            int punteggio = -Math.abs(
                    posto.getDistanzaUscita() - distanzaPreferita);

            if (migliore == null
                    || punteggio > punteggioMigliore
                    || (punteggio == punteggioMigliore
                        && confrontaPosti(posto, migliore) < 0)) {
                migliore = posto;
                punteggioMigliore = punteggio;
            }
        }

        return migliore;
    }

    /**
     * Confronta due posti per ottenere un ordinamento deterministico.
     *
     * @param a primo posto da confrontare
     * @param b secondo posto da confrontare
     * @return valore negativo, nullo o positivo secondo l'ordinamento
     */
    private int confrontaPosti(Posto a, Posto b) {
        int perPiano = Integer.compare(a.getPiano(), b.getPiano());
        return perPiano != 0
                ? perPiano
                : Integer.compare(a.getNumero(), b.getNumero());
    }

    /**
     * Converte una stringa in intero usando un valore di fallback in caso di errore.
     *
     * @param value valore testuale da convertire
     * @param defaultValue valore da usare in caso di conversione non valida
     * @return intero convertito oppure valore di fallback
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
