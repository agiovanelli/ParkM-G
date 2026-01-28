package pmg.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // Manteniamo il tuo pattern per localhost, ma rendiamolo più solido
                        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                        // AGGIUNTO: "PATCH" è vitale per l'emergenza!
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        // Cache della risposta preflight (evita che Chrome chieda il permesso a ogni click)
                        .maxAge(3600);
            }
        };
    }
}