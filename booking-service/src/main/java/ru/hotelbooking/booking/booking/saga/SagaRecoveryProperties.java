package ru.hotelbooking.booking.booking.saga;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * pendingTimeout - сколько бронь может висеть в PENDING, прежде чем её закроет SagaRecoveryJob
 * Должно быть больше самой долгой саги (5 кандидатов x 3 попытки x read-timeout).
 */
@ConfigurationProperties(prefix = "booking.saga-recovery")
public record SagaRecoveryProperties(
        @DefaultValue("5m") Duration pendingTimeout,
        @DefaultValue("50") int batchSize) {
}
