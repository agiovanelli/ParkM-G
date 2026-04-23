package pmg.backend.prenotazione;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pmg.backend.parcheggio.Parcheggio;
import pmg.backend.parcheggio.ParcheggioRepository;
import pmg.backend.utente.Utente;
import pmg.backend.utente.UtenteRepository;

@SpringBootTest
class CalcolaImportoIntegrationTest {

    @Autowired
    private PrenotazioneService prenotazioneService;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ParcheggioRepository parcheggioRepository;

    private Clock fixedClock;
    private LocalDateTime now;

    @BeforeEach
    void setup() {
        prenotazioneRepository.deleteAll();
        utenteRepository.deleteAll();
        parcheggioRepository.deleteAll();

        fixedClock = Clock.fixed(
                LocalDateTime.of(2024, 1, 15, 10, 0)
                        .atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        prenotazioneService.setClock(fixedClock);
        now = LocalDateTime.now(fixedClock);
    }

    @Test
    void calcolaImporto_senzaIngresso_returnZero() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");
        
        p.setDataCreazione(now.minusMinutes(5));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertEquals(0.0, result, 0.001);
    }

    @Test
    void calcolaImporto_base_unOra() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertEquals(3.0, result, 0.01);
    }

    @Test
    void calcolaImporto_scontoUtente() {
        Utente u = new Utente();
        u.setId("U1");
        u.setPreferenze(Map.of(
                "eta", "under30",
                "occupazione", "studente"
        ));
        utenteRepository.save(u);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");
        p.setDataCreazione(now.minusMinutes(10));
        p.setDataIngresso(now.minusHours(1));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertTrue(result > 2.0 && result < 3.0);
    }
    
    @Test
    void calcolaImporto_over60() {
        Utente u = new Utente();
        u.setId("U1");
        u.setPreferenze(Map.of("eta", "over60"));
        utenteRepository.save(u);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(2.4, result, 0.01);
    }
 
    @Test
    void calcolaImporto_notturno() {
        Clock nightClock = Clock.fixed(
            LocalDateTime.of(2024, 1, 15, 2, 0)
                .atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        prenotazioneService.setClock(nightClock);
        LocalDateTime nightNow = LocalDateTime.now(nightClock);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(nightNow.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.3, result, 0.01);
    }
    
    @Test
    void calcolaImporto_weekend() {
        Clock saturdayClock = Clock.fixed(
            LocalDateTime.of(2024, 1, 13, 10, 0)
                .atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        prenotazioneService.setClock(saturdayClock);
        LocalDateTime nowSat = LocalDateTime.now(saturdayClock);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(nowSat.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.6, result, 0.01);
    }
    
    @Test
    void calcolaImporto_feeMedia() {
        Parcheggio park = new Parcheggio();
        park.setId("PK1");
        park.setPostiTotali(100);
        park.setPostiDisponibili(40);
        parcheggioRepository.save(park);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(20));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertTrue(result > 3.0);
    }

    @Test
    void calcolaImporto_feeAttesa() {
        Parcheggio park = new Parcheggio();
        park.setId("PK1");
        park.setPostiTotali(100);
        park.setPostiDisponibili(10);
        parcheggioRepository.save(park);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(20));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertTrue(result > 3.0);
    }
    
    @Test
    void calcolaImporto_noFee() {
        Parcheggio park = new Parcheggio();
        park.setId("PK1");
        park.setPostiTotali(100);
        park.setPostiDisponibili(90);
        parcheggioRepository.save(park);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }

    @Test
    void calcolaImporto_penaleRitardo() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(30));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertTrue(result > 3.0);
    }

    @Test
    void calcolaImporto_scontoLungaDurata() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");
        p.setDataCreazione(now.minusHours(13));
        p.setDataIngresso(now.minusHours(13));

        Prenotazione saved = prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto(saved.getId());

        assertTrue(result > 25.0 && result < 35.0);
    }
    
    @Test
    void calcolaImporto_senzaIngresso() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");
        p.setDataCreazione(now.minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(0.0, result);
    }
    
    @Test
    void calcolaImporto_durataMinima() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusSeconds(10));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(1));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }
    
    @Test
    void calcolaImporto_utenteSenzaPreferenze() {
        Utente u = new Utente();
        u.setId("U1");
        u.setPreferenze(null);
        utenteRepository.save(u);

        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(1));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(3.0, result, 0.01);
    }
    
    @Test
    void calcolaImporto_sconto24h() {
        Prenotazione p = new Prenotazione();
        p.setId("P1");
        p.setUtenteId("U1");
        p.setParcheggioId("PK1");

        p.setDataIngresso(now.minusHours(25));
        p.setDataCreazione(p.getDataIngresso().minusMinutes(5));

        prenotazioneRepository.save(p);

        double result = prenotazioneService.calcolaImporto("P1");

        assertEquals(37.5, result, 0.01);
    }
    
    @Test
    void calcolaImporto_prenotazioneNonEsiste() {
        assertThrows(RuntimeException.class, () -> {
            prenotazioneService.calcolaImporto("NOT_FOUND");
        });
    }
}