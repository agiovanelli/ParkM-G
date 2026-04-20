package pmg.backend.posto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostoServiceImplTest {

    @Mock
    private PostoRepository postoRepository;

    @Mock
    private ParcheggioRepository parcheggioRepository;

    @InjectMocks
    private PostoServiceImpl postoService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPostiByParcheggio_senzaPiano() {
        Posto posto = new Posto("1", "p1", 1, 1, new PostoBoolean(true, false, false, false), 4);

        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of(posto));

        List<PostoResponse> result = postoService.getPostiByParcheggio("p1");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getNumero());
    }

    @Test
    void getPostiByParcheggio_conPiano() {
        Posto posto = new Posto("1", "p1", 5, 2, new PostoBoolean(true, false, false, false), 4);

        when(postoRepository.findByParcheggioIdAndPianoOrderByNumeroAsc("p1", 2))
                .thenReturn(List.of(posto));

        List<PostoResponse> result = postoService.getPostiByParcheggio("p1", 2);

        assertEquals(2, result.get(0).getPiano());
    }

    @Test
    void generaPosti_ok() {
        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of());

        postoService.generaPosti("p1");

        @SuppressWarnings("unchecked")
		ArgumentCaptor<List<Posto>> captor = ArgumentCaptor.forClass(List.class);
        verify(postoRepository).saveAll(captor.capture());

        List<Posto> salvati = captor.getValue();

        // 3 piani * 18 posti
        assertEquals(54, salvati.size());

        // controllo esempio
        Posto primo = salvati.get(0);
        assertTrue(primo.isDisponibile());
        assertFalse(primo.isDisabilitato());
    }

    @Test
    void generaPosti_giaEsistenti() {
        when(postoRepository.findByParcheggioIdOrderByPianoAscNumeroAsc("p1"))
                .thenReturn(List.of(new Posto()));

        assertThrows(IllegalStateException.class,
                () -> postoService.generaPosti("p1"));
    }

    @Test
    void aggiornaDisponibilita_cambiaValore() {
        Posto posto = new Posto("1", "p1", 1, 1, new PostoBoolean(true, false, false, false), 4);

        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setPostiDisponibili(4);

        when(postoRepository.findByParcheggioIdAndPianoAndNumero("p1", 1, 1))
                .thenReturn(Optional.of(posto));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        PostoResponse res = postoService.aggiornaDisponibilita("p1", 1, 1, false);

        assertFalse(res.isDisponibile());
        verify(parcheggioRepository).save(any());
    }

    @Test
    void aggiornaDisponibilita_postoNonTrovato() {
        when(postoRepository.findByParcheggioIdAndPianoAndNumero("p1", 1, 1))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> postoService.aggiornaDisponibilita("p1", 1, 1, true));
    }

    @Test
    void aggiornaDisabilitato_ok() {
        Posto posto = new Posto("1", "p1", 1, 1, new PostoBoolean(true, false, false, false), 4);

        Parcheggio parcheggio = new Parcheggio();
        parcheggio.setPostiDisponibili(5);
        parcheggio.setPostiTotali(100);

        when(postoRepository.findByParcheggioIdAndPianoAndNumero("p1", 1, 1))
                .thenReturn(Optional.of(posto));
        when(parcheggioRepository.findById("p1"))
                .thenReturn(Optional.of(parcheggio));

        PostoResponse res = postoService.aggiornaDisabilitato("p1", 1, 1, true);

        assertTrue(res.isDisabilitato());
        assertFalse(res.isDisponibile());

        verify(parcheggioRepository).save(any());
    }

    @Test
    void aggiornaDisabilitato_postoNonTrovato() {
        when(postoRepository.findByParcheggioIdAndPianoAndNumero("p1", 1, 1))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> postoService.aggiornaDisabilitato("p1", 1, 1, true));
    }
}