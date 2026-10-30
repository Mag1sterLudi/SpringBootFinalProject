package ru.hotelbooking.booking.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * baseUrl - имя сервиса в Eureka, readTimeout - на одну попытку,
 * maxCandidates - сколько номеров перебирать при автоподборе.
 */
@ConfigurationProperties(prefix = "booking.hotel-client")
public record HotelClientProperties(
        @DefaultValue("http://hotel-service") String baseUrl,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("5") int maxCandidates) {
}
