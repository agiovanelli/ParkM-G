package pmg.backend.prenotazione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/**
 * Test unitari del controller dedicato al ciclo di vita delle prenotazioni.
 */
@ExtendWith(MockitoExtension.class)
class PrenotazioneControllerTest {

    /** Servizio delle prenotazioni simulato. */
    @Mock
    private PrenotazioneService prenotazioneService;

    /** Controller sottoposto a test. */
    @InjectMocks
    private PrenotazioneController controller;

    /** Verifica il recupero dello storico di un utente. */
    @Test
    void getStorico_ok() {
        List<PrenotazioneResponse> lista = List.of(buildResponse());
        when(prenotazioneService.getStoricoUtente("u1"))
                .thenReturn(lista);

        ResponseEntity<List<PrenotazioneResponse>> response =
                controller.getStorico("u1");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(prenotazioneService).getStoricoUtente("u1");
    }

    /** Verifica la validazione dell'ingresso. */
    @Test
    void validaIngresso_ok() {
        PrenotazioneResponse expected = buildResponse();
        when(prenotazioneService.validaIngresso("QR"))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.validaIngresso("QR");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().id());
        verify(prenotazioneService).validaIngresso("QR");
    }

    /** Verifica il recupero della prenotazione tramite QR. */
    @Test
    void getPrenotazioneByQr_ok() {
        PrenotazioneResponse expected = buildResponse();
        when(prenotazioneService.getPrenotazioneByQr("QR"))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.getPrenotazioneByQr("QR");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("QR", response.getBody().codiceQr());
        verify(prenotazioneService).getPrenotazioneByQr("QR");
    }

    /** Verifica l'annullamento di una prenotazione. */
    @Test
    void annullaPrenotazione_ok() {
        PrenotazioneResponse expected = buildResponse();
        when(prenotazioneService.annullaPrenotazione("1", "u1"))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.annullaPrenotazione("1", "u1");

        assertEquals(200, response.getStatusCode().value());
        verify(prenotazioneService)
                .annullaPrenotazione("1", "u1");
    }

    /** Verifica il recupero dell'importo calcolato. */
    @Test
    void getImporto_ok() {
        when(prenotazioneService.calcolaImporto("1"))
                .thenReturn(12.5);

        ResponseEntity<Double> response = controller.getImporto("1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(12.5, response.getBody());
        verify(prenotazioneService).calcolaImporto("1");
    }

    /** Verifica la registrazione del pagamento. */
    @Test
    void paga_ok() {
        PrenotazioneResponse expected = buildResponse();
        Map<String, Double> payload = new HashMap<>();
        payload.put("importo", 20.0);

        when(prenotazioneService.pagaPrenotazione("1", 20.0))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.paga("1", payload);

        assertEquals(200, response.getStatusCode().value());
        verify(prenotazioneService).pagaPrenotazione("1", 20.0);
    }

    /** Verifica la validazione dell'uscita. */
    @Test
    void validaUscita_ok() {
        PrenotazioneResponse expected = buildResponse();
        when(prenotazioneService.validaUscita("QR"))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.validaUscita("QR");

        assertEquals(200, response.getStatusCode().value());
        verify(prenotazioneService).validaUscita("QR");
    }

    /** Verifica il recupero delle prenotazioni di un parcheggio. */
    @Test
    void getByParcheggio_ok() {
        List<PrenotazioneResponse> lista = List.of(buildResponse());
        when(prenotazioneService.getByParcheggio("p1"))
                .thenReturn(lista);

        ResponseEntity<List<PrenotazioneResponse>> response =
                controller.getByParcheggio("p1");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(prenotazioneService).getByParcheggio("p1");
    }

    /** Verifica la conferma del parcheggio del veicolo. */
    @Test
    void confermaParcheggio_ok() {
        PrenotazioneResponse expected = buildResponse();
        when(prenotazioneService.confermaParcheggio("1"))
                .thenReturn(expected);

        ResponseEntity<PrenotazioneResponse> response =
                controller.confermaParcheggio("1");

        assertEquals(200, response.getStatusCode().value());
        verify(prenotazioneService).confermaParcheggio("1");
    }

    /** Crea una risposta sintetica comune ai test del controller. */
    private PrenotazioneResponse buildResponse() {
        return new PrenotazioneResponse(
                "1",
                "u1",
                "p1",
                null,
                "QR",
                StatoPrenotazione.attiva,
                null,
                null,
                0.0,
                null,
                null);
    }
}
