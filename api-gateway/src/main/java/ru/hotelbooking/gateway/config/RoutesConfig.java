package ru.hotelbooking.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutesConfig {

    // шаги саги вызывает только booking-service напрямую, наружу их не отдаём
    static final String[] INTERNAL_HOTEL_PATHS = {
        "/api/rooms/*/confirm-availability",
        "/api/rooms/*/release"
    };

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("booking-service", route -> route
                        .path("/api/bookings/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri("lb://booking-service"))
                .route("hotel-service", route -> route
                        .path("/api/hotels/**", "/api/rooms/**")
                        .and()
                        .not(internal -> internal.path(INTERNAL_HOTEL_PATHS))
                        .uri("lb://hotel-service"))
                .build();
    }
}
