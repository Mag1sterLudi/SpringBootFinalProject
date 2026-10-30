# booking-service

Пользователи, выдача JWT и бронирования. Порт 8082, Swagger UI - http://localhost:8082/swagger-ui.html.

| Пакет | Что внутри |
|---|---|
| `user` | регистрация, вход, управление пользователями (ADMIN) |
| `security` | выпуск токенов, `CurrentUser` из JWT |
| `booking` | API бронирований, идемпотентность, история, рекомендации |
| `booking.saga` | `BookingSaga` (шаги саги), `BookingStateService` (переходы статусов), `CompensationService`, `SagaRecoveryJob` (повтор release, закрытие зависших броней) |
| `client` | HTTP-клиент к hotel-service: Eureka, тайм-ауты, Retry, CircuitBreaker |
| `seed` | демо-данные |

Статусы брони:

- `PENDING -> CONFIRMED` - отель заблокировал номер;
- `PENDING -> CANCELLED` - отель ответил 409;
- `PENDING -> CANCELLED` + release - тайм-аут или ошибка отеля после повторов;
- `CONFIRMED -> CANCELLED` + release - пользователь отменил бронь (`DELETE /booking/{id}`);
- `PENDING -> CANCELLED` + release - бронь висела в `PENDING` дольше `pending-timeout` (`PROCESSING_TIMEOUT`).

Если release не прошёл, у брони остаётся `release_pending = true`, и `SagaRecoveryJob` повторяет его позже.

Основные настройки:

| Свойство | По умолчанию |
|---|---|
| `booking.hotel-client.read-timeout` | `3s` |
| `booking.hotel-client.max-candidates` | `5` - сколько номеров пробовать при автоподборе |
| `resilience4j.retry.instances.hotel.max-attempts` | `3`, пауза 300 мс, потом 600 мс |
| `resilience4j.circuitbreaker.instances.hotel.*` | окно 10 вызовов, порог 50 % |
| `booking.saga-recovery.interval` | `30s` - как часто запускается `SagaRecoveryJob` |
| `booking.saga-recovery.pending-timeout` | `5m` |
| `security.jwt.access-token-ttl` | `1h` |
