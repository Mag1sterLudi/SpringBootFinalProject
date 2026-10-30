package ru.hotelbooking.booking.security;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.hotelbooking.booking.user.Role;
import ru.hotelbooking.common.security.TokenClaims;

/**
 * Текущий пользователь, берётся из claims токена без обращения к БД.
 */
public record CurrentUser(Long id, String username, Role role) {

    public static CurrentUser from(Jwt jwt) {
        Object userId = jwt.getClaim(TokenClaims.USER_ID);
        if (!(userId instanceof Number number)) {
            throw new UnauthorizedException("USER_TOKEN_REQUIRED", "Token does not belong to a user");
        }
        List<String> roles = jwt.getClaimAsStringList(TokenClaims.ROLES);
        Role role = roles != null && roles.contains(Role.ADMIN.name()) ? Role.ADMIN : Role.USER;
        return new CurrentUser(number.longValue(), jwt.getSubject(), role);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
