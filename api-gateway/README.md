# api-gateway

Spring Cloud Gateway, порт 8080. Единая точка входа для клиентов.

Маршруты (`RoutesConfig`), адреса сервисов берутся из Eureka:

| Путь | Сервис |
|---|---|
| `/api/bookings/**` | booking-service, префикс `/api/bookings` отрезается |
| `/api/hotels/**`, `/api/rooms/**` | hotel-service |
| `/api/rooms/*/confirm-availability`, `/api/rooms/*/release` | не публикуются (404) |

`GatewaySecurityConfig` проверяет JWT на входе. Без токена доступны только регистрация,
вход и `/actuator/health`. Роли дополнительно проверяют сами сервисы.

`AccessLogFilter` пишет в лог метод, путь, статус и время каждого запроса (в том числе отклонённых
с 401/403). traceId создаёт Micrometer Tracing и передаёт его в сервисы. `TraceIdResponseFilter`
возвращает его клиенту в заголовке `X-Trace-Id`, он же есть в теле ошибок шлюза
(`JsonSecurityErrorHandler` для 401/403, `GatewayErrorAttributes` для 404/503), формат ошибки такой
же, как у сервисов.
JSON-лог пишется в `logs/api-gateway.log`.
