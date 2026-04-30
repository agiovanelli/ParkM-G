package pmg.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principale dell'applicazione backend PMG.
 *
 * Avvia l'applicazione Spring Boot e abilita l'esecuzione
 * delle attività schedulate.
 */
@SpringBootApplication
@EnableScheduling
public class PmgBackendApplication {

	/**
     * Punto di ingresso dell'applicazione.
     *
     * @param args argomenti passati da linea di comando
     */
    public static void main(String[] args) {
        SpringApplication.run(PmgBackendApplication.class, args);
    }
}