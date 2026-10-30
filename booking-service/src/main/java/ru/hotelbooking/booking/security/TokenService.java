package ru.hotelbooking.booking.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.hotelbooking.booking.user.User;
import ru.hotelbooking.booking.user.dto.AuthResponse;
import ru.hotelbooking.common.security.JwtProperties;
import ru.hotelbooking.common.security.TokenClaims;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String INTERNAL_ROLE = "INTERNAL";
    private static final String SERVICE_SUBJECT = "booking-service";
    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public AuthResponse issueFor(User user) {
        Duration ttl = properties.accessTokenTtl();
        String token = issue(user.getUsername(), user.getRole().name(), ttl,
                Map.of(TokenClaims.USER_ID, user.getId()));
        return new AuthResponse(token, TOKEN_TYPE, ttl.toSeconds(), user.getUsername(), user.getRole());
    }

    // для confirm-availability / release в hotel-service
    public String issueServiceToken() {
        return issue(SERVICE_SUBJECT, INTERNAL_ROLE, SERVICE_TOKEN_TTL, Map.of());
    }

    private String issue(String subject, String role, Duration ttl, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(TokenClaims.ROLES, List.of(role));
        extraClaims.forEach(claims::claim);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
