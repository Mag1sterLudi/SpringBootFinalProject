# common

Общий код для hotel-service и booking-service, чтобы не дублировать его в каждом сервисе.
Подключается как Maven-зависимость. Бины попадают в контекст, потому что сервисы сканируют
пакет `ru.hotelbooking.common` (`scanBasePackages` в `@SpringBootApplication`).

- `error` - единый формат ошибки `ApiError`, исключения `NotFoundException`, `ConflictException`
  и др., общий `GlobalExceptionHandler`.
- `security` - настройки JWT (`security.jwt.*`), проверка токена, ответы 401/403 в формате `ApiError`.
- `validation` - аннотация `@ValidDateRange` для проверки дат заезда и выезда.
- `web` - `PageResponse` для постраничных ответов и настройка Swagger с авторизацией по токену.

Gateway работает на WebFlux, поэтому этот модуль не использует.
