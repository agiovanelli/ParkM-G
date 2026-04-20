package pmg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    void corsConfigurer_notNull() {
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();

        assertNotNull(configurer);
    }

    @Test
    void addCorsMappings_doesNotThrow() {
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        CorsRegistry registry = new CorsRegistry();

        // Serve per coprire il blocco lambda/override
        assertDoesNotThrow(() -> configurer.addCorsMappings(registry));
    }
}