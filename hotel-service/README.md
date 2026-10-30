# hotel-service

Управление отелями и номерами, блокировка слотов (участник саги), статистика загруженности. Порт 8081,
Swagger UI - http://localhost:8081/swagger-ui.html.

## Пакеты

| Пакет | Что внутри |
|---|---|
| `hotel` | CRUD отелей, фильтры через JPA Specifications |
| `room` | CRUD номеров, поиск свободных номеров на даты, рекомендации (`times_booked ASC, id ASC`) |
| `reservation` | `confirm-availability` / `release`: пессимистическая блокировка номера, идемпотентность по `requestId` |
| `statistics` | загруженность по номеро-ночам, равномерность распределения (`OccupancyCalculator`) |
| `seed` | демо-данные |

## Статистика

`GET /api/hotels/{id}/statistics?from=&to=` (по умолчанию 30 дней от сегодняшнего дня):

- `occupancyRate` - доля занятых номеро-ночей среди номеров в эксплуатации;
- `averageTimesBooked` - среднее число бронирований на номер;
- `idleRoomIds` - номера без единой занятой ночи за период;
- `rooms[]` - то же по каждому номеру.

`GET /api/hotels/statistics` - сводка по всем отелям, от самых загруженных.

## Настройки

| Свойство | По умолчанию |
|---|---|
| `server.port` | `8081` (`SERVER_PORT`) |
| `security.jwt.secret` | `JWT_SECRET` |
| `app.seed.enabled` | `true` |
