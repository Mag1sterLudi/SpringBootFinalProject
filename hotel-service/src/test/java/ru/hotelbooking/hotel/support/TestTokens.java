package ru.hotelbooking.hotel.support;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import ru.hotelbooking.common.security.JwtProperties;
import ru.hotelbooking.common.security.TokenClaims;

/**
 * Настоящие подписанные токены, чтобы проверить, что сервис сам валидирует JWT.
 */
public final class TestTokens {

    private TestTokens() {
    }

    public static String issue(JwtProperties properties, String role, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(properties.secretKey()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject("test-" + role.toLowerCase())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TokenClaims.ROLES, List.of(role))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
