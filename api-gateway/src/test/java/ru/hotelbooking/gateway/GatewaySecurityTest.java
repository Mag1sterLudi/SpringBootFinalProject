package ru.hotelbooking.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.hotelbooking.gateway.filter.TraceIdResponseFilter;
import ru.hotelbooking.gateway.security.GatewayJwtProperties;

/**
 * Backend-сервисы в тесте не подняты, поэтому успешная маршрутизация
 * проявляется как 503 от балансировщика (нет экземпляров), а не 401/404.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "logging.file.name="})
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GatewayJwtProperties jwtProperties;

    @Test
    @DisplayName("Запрос без токена отклоняется шлюзом с 401 и JSON-ошибкой")
    void requestWithoutTokenIsRejected() {
        webTestClient.get().uri("/api/hotels")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.path").isEqualTo("/api/hotels")
                .jsonPath("$.traceId").isNotEmpty();
    }

    @Test
    @DisplayName("Регистрация и вход доступны без токена")
    void authEndpointsArePublic() {
        webTestClient.post().uri("/api/bookings/user/auth")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("С валидным токеном запрос маршрутизируется в hotel-service")
    void authenticatedRequestIsRouted() {
        webTestClient.get().uri("/api/rooms")
                .headers(headers -> headers.setBearerAuth(token("USER")))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Внутренние эндпойнты Hotel Service через шлюз не публикуются")
    void internalHotelEndpointsAreNotExposed() {
        webTestClient.post().uri("/api/rooms/1/confirm-availability")
                .headers(headers -> headers.setBearerAuth(token("ADMIN")))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.traceId").isNotEmpty()
                .jsonPath("$.requestId").doesNotExist();

        webTestClient.post().uri("/api/rooms/1/release")
                .headers(headers -> headers.setBearerAuth(token("ADMIN")))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("traceId возвращается клиенту в заголовке X-Trace-Id и в теле ошибки")
    void traceIdIsReturnedInHeader() {
        webTestClient.get().uri("/api/hotels")
                .exchange()
                .expectHeader().valueMatches(TraceIdResponseFilter.HEADER, "[0-9a-f]{32}")
                .expectBody()
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).asString().hasSize(32));
    }

    private String token(String role) {
        ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(jwtProperties.secretKey());
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(secret);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject("gateway-test")
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
