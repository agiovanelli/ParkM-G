package pmg.backend;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PmgBackendApplicationTest {

    @Test
    void contextLoads() {
        // Test vuoto: verifica che il contesto Spring parta
    }

    @Test
    void mainMethod_invokesSpring() {
        try (var mocked = mockStatic(org.springframework.boot.SpringApplication.class)) {

            PmgBackendApplication.main(new String[]{});

            mocked.verify(() ->
                    org.springframework.boot.SpringApplication.run(
                            PmgBackendApplication.class,
                            new String[]{}
                    )
            );
        }
    }
}