package pmg.backend.prenotazione;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrenotazioneControllerTest {

    @Mock
    private PrenotazioneService prenotazioneService;

    @InjectMocks
    private PrenotazioneController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private PrenotazioneResponse buildResponse() {
        return new PrenotazioneResponse("1", "u1", "p1", null, "QR", StatoPrenotazione.attiva, null, null, 0.0, null, null);
    }

    @Test
    void getStorico_ok() {
        List<PrenotazioneResponse> list = List.of(buildResponse());

        when(prenotazioneService.getStoricoUtente("u1")).thenReturn(list);

        ResponseEntity<List<PrenotazioneResponse>> res = controller.getStorico("u1");

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
        verify(prenotazioneService).getStoricoUtente("u1");
    }

    @Test
    void validaIngresso_ok() {
        PrenotazioneResponse response = buildResponse();

        when(prenotazioneService.validaIngresso("QR")).thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res = controller.validaIngresso("QR");

        assertEquals(200, res.getStatusCode().value());
        assertEquals("1", res.getBody().id());
        verify(prenotazioneService).validaIngresso("QR");
    }

    @Test
    void getPrenotazioneByQr_ok() {
        PrenotazioneResponse response = buildResponse();

        when(prenotazioneService.getPrenotazioneByQr("QR")).thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res = controller.getPrenotazioneByQr("QR");

        assertEquals(200, res.getStatusCode().value());
        assertEquals("QR", res.getBody().codiceQr());
        verify(prenotazioneService).getPrenotazioneByQr("QR");
    }

    @Test
    void annullaPrenotazione_ok() {
        PrenotazioneResponse response = buildResponse();

        when(prenotazioneService.annullaPrenotazione("1", "u1"))
                .thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res =
                controller.annullaPrenotazione("1", "u1");

        assertEquals(200, res.getStatusCode().value());
        verify(prenotazioneService).annullaPrenotazione("1", "u1");
    }

    @Test
    void getImporto_ok() {
        when(prenotazioneService.calcolaImporto("1")).thenReturn(12.5);

        ResponseEntity<Double> res = controller.getImporto("1");

        assertEquals(200, res.getStatusCode().value());
        assertEquals(12.5, res.getBody());
        verify(prenotazioneService).calcolaImporto("1");
    }

    @Test
    void paga_ok() {
        PrenotazioneResponse response = buildResponse();

        Map<String, Double> payload = new HashMap<>();
        payload.put("importo", 20.0);

        when(prenotazioneService.pagaPrenotazione("1", 20.0))
                .thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res =
                controller.paga("1", payload);

        assertEquals(200, res.getStatusCode().value());
        verify(prenotazioneService).pagaPrenotazione("1", 20.0);
    }

    @Test
    void validaUscita_ok() {
        PrenotazioneResponse response = buildResponse();

        when(prenotazioneService.validaUscita("QR")).thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res =
                controller.validaUscita("QR");

        assertEquals(200, res.getStatusCode().value());
        verify(prenotazioneService).validaUscita("QR");
    }

    @Test
    void getByParcheggio_ok() {
        List<PrenotazioneResponse> list = List.of(buildResponse());

        when(prenotazioneService.getByParcheggio("p1")).thenReturn(list);

        ResponseEntity<List<PrenotazioneResponse>> res =
                controller.getByParcheggio("p1");

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
        verify(prenotazioneService).getByParcheggio("p1");
    }

    @Test
    void confermaParcheggio_ok() {
        PrenotazioneResponse response = buildResponse();

        when(prenotazioneService.confermaParcheggio("1"))
                .thenReturn(response);

        ResponseEntity<PrenotazioneResponse> res =
                controller.confermaParcheggio("1");

        assertEquals(200, res.getStatusCode().value());
        verify(prenotazioneService).confermaParcheggio("1");
    }
}