# Task Tracker API

REST API для учёта задач в команде с аутентификацией (JWT) и разграничением прав
доступа. Пользователи регистрируются, входят по email/паролю и работают со своими
задачами или задачами, где они участвуют.

## Технологии

- Java 21, Spring Boot 3.5
- Spring Web, Spring Security (JWT, HS256)
- Spring Data JPA + PostgreSQL
- Flyway (миграции)
- springdoc-openapi (Swagger UI)
- Maven, JUnit 5 + Mockito + MockMvc (тесты на H2)
- Docker / Docker Compose

## Требования

- **JDK 21** (для локального запуска; используется Maven Wrapper `./mvnw`)
- **PostgreSQL** (для локального запуска без Docker)
- **Docker + Docker Compose** (для запуска через контейнеры)

## Локальный запуск

1. Создать базу данных (Flyway создаёт только таблицы, но не саму БД):

   ```sql
   CREATE DATABASE task_tracker;
   ```

2. Настроить подключение. По умолчанию приложение берёт значения из
   `application.properties`, но их можно переопределить переменными окружения:

   | Переменная | Значение по умолчанию |
   |------------|-----------------------|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/task_tracker` |
   | `SPRING_DATASOURCE_USERNAME` | `postgres` |
   | `SPRING_DATASOURCE_PASSWORD` | `postgres` |
   | `JWT_SECRET` | (dev-значение) |
   | `JWT_EXPIRATION` | `3600` (секунды) |

3. Запустить:

   ```bash
   cd task-tracker-api
   ./mvnw spring-boot:run
   ```

   Приложение стартует на `http://localhost:8080`, Flyway применит миграции.

4. (Опционально) Seed-данные для разработки — профиль `dev` создаёт одного ADMIN
   и двух USER:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   | Email | Пароль | Роль |
   |-------|--------|------|
   | `admin@example.com` | `SecurePass123` | ADMIN |
   | `user1@example.com` | `SecurePass123` | USER |
   | `user2@example.com` | `SecurePass123` | USER |

## Запуск через Docker Compose

Из корня репозитория:

```bash
cp .env.example .env      # заполнить значения
docker compose up --build
```

Поднимаются два контейнера: PostgreSQL и приложение (multi-stage Dockerfile).
Приложение доступно на `http://localhost:8080`. PostgreSQL проброшен на хост-порт
**5433** (чтобы не конфликтовать с локально установленным PostgreSQL на 5432).

## Примеры запросов (curl)

> На Windows/PowerShell используйте `curl.exe` или Swagger UI.

**1. Регистрация** → 201, возвращает `accessToken` и данные пользователя:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass123","name":"Alice"}'
```

**2. Вход** → 200, возвращает `accessToken`:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass123"}'
```

**3. Создание задачи** (подставьте токен из ответа выше):

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"title":"Настроить CI","description":"Добавить pipeline","priority":"HIGH"}'
```

**4. Получение задачи:**

```bash
curl http://localhost:8080/api/v1/tasks/<TASK_ID> \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Swagger UI

- UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Нажмите **Authorize** и вставьте JWT, чтобы вызывать защищённые эндпоинты.

## Принятые архитектурные решения

- **Слоистая архитектура:** `controller → service → repository`.
- **DTO отделены от Entity** — наружу сущности не отдаются; маппинг ручными мапперами.
- **JWT + stateless:** сессии не создаются, каждый запрос аутентифицируется по токену;
  роль и id пользователя лежат в payload.
- **Единый формат ошибок** через `@RestControllerAdvice` (`ApiError`); 401/403 из
  фильтров Security возвращаются в том же формате.
- **Фильтрация задач** реализована через JPA `Specification` (динамические условия),
  для USER результат автоматически ограничивается его задачами.
- **`@Transactional`** на методах сервиса задач — чтобы ленивые связи `creator`/
  `assignee` можно было безопасно читать при маппинге.
- **Конфигурация через переменные окружения** (секреты не хардкодятся в репозитории).
- **Миграции Flyway** описывают схему; `ddl-auto=validate` в проде.

## Что можно улучшить при большем сроке

- Интеграционные тесты на **Testcontainers** (реальный PostgreSQL) вместо H2.
- **Refresh-токены** и эндпоинт `POST /api/v1/auth/refresh`.
- **История изменений** задач (таблица `task_audit`).
- **Кэширование** `GET /tasks/{id}` с инвалидацией при изменениях.
- **Rate limiting** на `/auth/login` (защита от brute force).
- Возможность **снять исполнителя** через PATCH (сейчас `null` = «поле не передано»).
- Семантическая сортировка по `priority` (сейчас — лексикографическая по enum).
- Переход на **MapStruct** и детальные OpenAPI-аннотации на эндпоинтах.
```
