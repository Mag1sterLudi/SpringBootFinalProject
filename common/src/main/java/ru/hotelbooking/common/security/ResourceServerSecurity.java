package ru.hotelbooking.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * То, что одинаково в обоих сервисах - stateless, JWT, ответы 401/403.
 * Правила по URL каждый сервис пишет в своём SecurityConfig.
 */
@RequiredArgsConstructor
public class ResourceServerSecurity {

    public static final String[] PUBLIC_PATHS = {
        "/error",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health/**",
        "/actuator/info"
    };

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final RestSecurityErrorHandler errorHandler;

    public HttpSecurity applyDefaults(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
    }
}
