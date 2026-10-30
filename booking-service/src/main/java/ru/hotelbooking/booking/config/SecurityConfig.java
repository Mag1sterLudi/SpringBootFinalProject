package ru.hotelbooking.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotelbooking.common.security.ResourceServerSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer)
            throws Exception {
        resourceServer.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/user/register", "/user/auth").permitAll()
                        .requestMatchers(ResourceServerSecurity.PUBLIC_PATHS).permitAll()
                        .requestMatchers("/user", "/user/**").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("USER", "ADMIN"));
        return http.build();
    }
}
