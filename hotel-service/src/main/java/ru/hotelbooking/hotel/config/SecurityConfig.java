package ru.hotelbooking.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotelbooking.common.security.ResourceServerSecurity;

// Общие правила по URL здесь, точные - через @PreAuthorize на контроллерах
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer)
            throws Exception {
        resourceServer.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ResourceServerSecurity.PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rooms/*/confirm-availability", "/api/rooms/*/release")
                        .hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN", "INTERNAL")
                        .anyRequest().hasRole("ADMIN"));
        return http.build();
    }
}
