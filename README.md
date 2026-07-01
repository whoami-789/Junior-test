# Тестовое задание: Task Tracker API

---

## 1. Цель задания

Разработать REST API для учёта задач в команде с аутентификацией и авторизацией. Задание проверяет:

- проектирование REST API и работу со Spring Boot;
- JPA, миграции, транзакции;
- валидацию и единообразную обработку ошибок;
- JWT-аутентификацию и разграничение прав доступа;
- unit- и integration-тесты;
- Docker, документацию API.

---

## 2. Бизнес-контекст

Команда ведёт задачи с приоритетом и статусом. Пользователи регистрируются в системе, входят по email и паролю и работают только со своими данными или с задачами, где они участвуют. Backend должен быть готов к подключению UI.

---

## 3. Функциональные требования

### 3.1. Сущности

#### User

| Поле        | Тип        | Описание                          |
|-------------|------------|-----------------------------------|
| `id`        | UUID       | Первичный ключ                    |
| `email`     | String     | Уникальный, используется для входа |
| `password`  | String     | Хэш пароля (BCrypt), не отдавать в API |
| `name`      | String     | Отображаемое имя                  |
| `role`      | Enum       | `USER` или `ADMIN`                |
| `createdAt` | Instant    | Дата создания                     |

#### Task

| Поле          | Тип        | Описание                              |
|---------------|------------|---------------------------------------|
| `id`          | UUID       | Первичный ключ                        |
| `title`       | String     | Обязательно, 3–200 символов           |
| `description` | String     | Опционально, до 5000 символов         |
| `status`      | Enum       | `TODO`, `IN_PROGRESS`, `DONE`         |
| `priority`    | Enum       | `LOW`, `MEDIUM`, `HIGH`               |
| `creator`     | User       | Кто создал задачу (обязательно)       |
| `assignee`    | User       | Исполнитель (опционально)             |
| `createdAt`   | Instant    | Дата создания                       |
| `updatedAt`   | Instant    | Дата последнего изменения           |

---

### 3.2. Аутентификация и авторизация

#### Регистрация и вход

| Метод | Endpoint              | Доступ   | Описание                    |
|-------|-----------------------|----------|-----------------------------|
| POST  | `/api/v1/auth/register` | Публичный | Регистрация нового пользователя |
| POST  | `/api/v1/auth/login`    | Публичный | Вход, выдача JWT            |
| GET   | `/api/v1/auth/me`       | Авторизованный | Текущий пользователь   |

#### POST `/api/v1/auth/register`

**Тело запроса:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "name": "Иван Иванов"
}
```

**Правила:**

- `email` — валидный формат, уникален → при дубликате **409 Conflict**.
- `password` — минимум 8 символов, хотя бы одна буква и одна цифра.
- `name` — 2–100 символов.
- При регистрации роль по умолчанию — `USER`.
- Пароль хранить только в виде BCrypt-хэша.
- Ответ: **201 Created** + DTO пользователя (без пароля) + JWT access token.

#### POST `/api/v1/auth/login`

**Тело запроса:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Правила:**

- При неверных credentials → **401 Unauthorized** (без уточнения, email или пароль неверный).
- При успехе → JWT access token + DTO пользователя.

#### Формат ответа auth-эндпоинтов

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "Иван Иванов",
    "role": "USER",
    "createdAt": "2026-07-01T12:00:00Z"
  }
}
```

#### JWT

- Access token срок жизни: **1 час**.
- Алгоритм: **HS256** (секрет из `application.yml` / env-переменной).
- В payload минимум: `sub` (user id), `email`, `role`, `exp`, `iat`.
- Все защищённые эндпоинты принимают заголовок: `Authorization: Bearer <token>`.
- При отсутствии или невалидном токене → **401 Unauthorized**.
- При валидном токене, но недостаточных правах → **403 Forbidden**.

#### Публичные эндпоинты (без JWT)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /actuator/health` (если подключён Actuator)
- `GET /swagger-ui/**`, `GET /v3/api-docs/**`

Все остальные эндпоинты — только для авторизованных пользователей.

---

### 3.3. Правила авторизации (доступ к ресурсам)

#### Роль `USER`

| Действие | Условие доступа |
|----------|-----------------|
| `GET /users/{id}` | Только свой профиль |
| `GET /users` | Запрещено → **403** |
| `POST /tasks` | Разрешено; `creator` = текущий пользователь |
| `GET /tasks/{id}` | Задача, где пользователь — `creator` или `assignee` |
| `PATCH /tasks/{id}` | Пользователь — `creator` или `assignee` |
| `DELETE /tasks/{id}` | Только `creator` |
| `GET /tasks` | Только задачи, где пользователь — `creator` или `assignee` |

#### Роль `ADMIN`

- Полный доступ ко всем пользователям и задачам.
- Может назначать `assignee` любому существующему пользователю.
- Может удалять любые задачи.

#### Дополнительные ограничения при `PATCH /tasks/{id}`

| Поле            | Кто может менять                          |
|-----------------|-------------------------------------------|
| `status`        | `creator`, `assignee` или `ADMIN`         |
| `priority`      | `creator` или `ADMIN`                     |
| `title`, `description` | `creator` или `ADMIN`              |
| `assigneeId`    | `creator` или `ADMIN`                     |

Попытка изменить поле без прав → **403 Forbidden** с понятным сообщением.

---

### 3.4. API пользователей

Базовый путь: `/api/v1`

| Метод | Endpoint        | Доступ        | Описание              |
|-------|-----------------|---------------|-----------------------|
| GET   | `/users/{id}`   | Auth          | Получить пользователя |
| GET   | `/users`        | ADMIN         | Список с пагинацией   |

**GET `/users` (только ADMIN):**

- Пагинация: `page` (default 0), `size` (default 20).
- Сортировка: `sort=createdAt,desc`.

---

### 3.5. API задач

| Метод  | Endpoint       | Доступ | Описание                    |
|--------|----------------|--------|-----------------------------|
| POST   | `/tasks`       | Auth   | Создать задачу              |
| GET    | `/tasks/{id}`  | Auth   | Получить задачу             |
| PATCH  | `/tasks/{id}`  | Auth   | Частичное обновление        |
| DELETE | `/tasks/{id}`  | Auth   | Удалить задачу              |
| GET    | `/tasks`       | Auth   | Список с фильтрами          |

#### POST `/tasks`

**Тело запроса:**

```json
{
  "title": "Настроить CI",
  "description": "Добавить pipeline в GitLab",
  "priority": "HIGH",
  "assigneeId": "uuid-опционально"
}
```

**Правила:**

- `creator` устанавливается автоматически из JWT (не передаётся в теле).
- Статус по умолчанию — `TODO`.
- Если указан `assigneeId` — пользователь должен существовать, иначе **404**.
- Обычный `USER` может назначить `assignee` только на себя; `ADMIN` — на любого.

#### PATCH `/tasks/{id}`

Частичное обновление. Передаются только изменяемые поля:

```json
{
  "title": "Новый заголовок",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "assigneeId": "uuid"
}
```

#### GET `/tasks` — фильтры и пагинация

| Параметр      | Описание                                           |
|---------------|----------------------------------------------------|
| `status`      | Один или несколько: `TODO`, `IN_PROGRESS`, `DONE`  |
| `priority`    | `LOW`, `MEDIUM`, `HIGH`                            |
| `assigneeId`  | UUID исполнителя                                   |
| `creatorId`   | UUID создателя (только для ADMIN)                  |
| `search`      | Поиск по `title` (case-insensitive, contains)      |
| `sort`        | Например `createdAt,desc` или `priority,asc`       |
| `page`        | Номер страницы (default: 0)                        |
| `size`        | Размер страницы (default: 20, max: 100)            |

Для `USER` результат автоматически ограничивается задачами, где он `creator` или `assignee`.

---

### 3.6. Бизнес-правила

1. Email пользователя уникален → **409** при дубликате.
2. Нельзя назначить несуществующего `assignee` → **404**.
3. Нельзя перевести задачу в `DONE`, если нет `assignee` → **400**:
   ```json
   {
     "message": "Cannot mark task as DONE without assignee"
   }
   ```
4. `updatedAt` обновляется при любом изменении задачи.
5. Удалённую задачу получить нельзя → **404**.
6. Доступ к чужой задаче без прав → **403** (не **404**, чтобы не раскрывать существование ресурса).

---

### 3.7. Формат ошибок (единый для всего API)

```json
{
  "timestamp": "2026-07-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot mark task as DONE without assignee",
  "path": "/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000"
}
```

Для ошибок валидации (400) допускается расширенный формат:

```json
{
  "timestamp": "2026-07-01T12:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request",
  "path": "/api/v1/auth/register",
  "errors": [
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ]
}
```

---

## 4. Нефункциональные требования

### 4.1. Технологии

| Компонент        | Требование                          |
|------------------|-------------------------------------|
| Java             | 17+                                 |
| Spring Boot      | 3.x                                 |
| БД               | PostgreSQL                          |
| Миграции         | Flyway или Liquibase                |
| Auth             | Spring Security + JWT               |
| Документация API | springdoc-openapi (Swagger UI)      |
| Сборка           | Maven или Gradle                    |
| Логирование      | SLF4J + Logback                     |

### 4.2. Архитектура

- Разделение слоёв: `controller` → `service` → `repository`.
- **DTO** отдельно от **Entity** — наружу entity не отдавать.
- Маппинг: MapStruct или ручной mapper.
- Централизованная обработка исключений: `@ControllerAdvice`.
- Конфигурация Security вынесена в отдельный класс (`SecurityConfig`).

**Рекомендуемая структура пакетов:**

```
com.company.tasktracker
├── config
│   └── SecurityConfig
├── controller
├── dto
│   ├── auth
│   ├── task
│   └── user
├── model
├── exception
├── mapper
├── repository
├── security
│   ├── JwtService
│   ├── JwtAuthFilter
│   └── UserDetailsServiceImpl
└── service
```

### 4.3. Безопасность

- Пароли — только BCrypt, cost factor ≥ 10.
- JWT secret — из переменной окружения `JWT_SECRET`, не хардкодить в репозитории.
- В логах не выводить пароли и токены.
- CSRF отключить (stateless REST API).
- CORS: базовая настройка для `http://localhost:3000` (на будущий фронт).

### 4.4. Docker

`docker-compose.yml` должен поднимать:

- PostgreSQL;
- приложение (multi-stage Dockerfile).

Запуск одной командой:

```bash
docker compose up --build
```

Переменные окружения через `.env.example` (без реальных секретов).

### 4.5. README

Обязательные разделы:

1. Описание проекта.
2. Требования (Java, Docker).
3. Локальный запуск.
4. Запуск через Docker Compose.
5. Примеры запросов (register → login → create task) с `curl`.
6. Ссылка на Swagger UI.
7. Принятые архитектурные решения.
8. Что бы улучшили при большем сроке.

---

## 5. Тесты

### 5.1. Обязательный минимум

| # | Тип         | Сценарий |
|---|-------------|----------|
| 1 | Unit        | Нельзя перевести задачу в `DONE` без `assignee` |
| 2 | Unit        | Регистрация с дублирующимся email → исключение / 409 |
| 3 | Unit        | `USER` не может удалить чужую задачу |
| 4 | Integration | Register → Login → Create task → Get task (happy path) |
| 5 | Integration | Запрос без JWT к `GET /tasks` → 401 |
| 6 | Integration | `USER` пытается получить чужую задачу → 403 |
| 7 | Integration | `PATCH` статуса в `DONE` без assignee → 400 |
| 8 | Integration | `GET /tasks?status=TODO&priority=HIGH` с фильтрацией |

---

## 8. Что сдать

1. Ссылка на репозиторий (GitHub / GitLab), публичный или с выданным доступом.
2. Рабочий `docker compose up`.
3. README по разделу 4.5.

---

## 9. Опциональные улучшения (бонус)

Не обязательны, но дают преимущество при равных результатах:

1. **Refresh token** — отдельный долгоживущий токен и эндпоинт `POST /api/v1/auth/refresh`.
2. **История изменений** — таблица `task_audit`: кто, когда, какое поле изменил.
3. **Кэш** — `@Cacheable` для `GET /tasks/{id}`, инвалидация при PATCH/DELETE.
4. **Rate limiting** на `/auth/login` (защита от brute force).
5. **Seed-данные** — один ADMIN и несколько USER при старте (profile `dev`).
