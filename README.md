# Explore With Me

Приложение-афиша, позволяющее пользователям делиться информацией об интересных событиях и находить компанию для участия в них.

## Описание проекта

Explore With Me — это платформа для организации и участия в различных событиях. Пользователи могут создавать события, регистрироваться на них, формировать подборки и оставлять комментарии.

## Архитектура

Проект построен на **микросервисной архитектуре** с использованием Spring Cloud и включает следующие компоненты:

### Инфраструктурные сервисы

- **Discovery Server** (`discovery-server`) — сервис регистрации и обнаружения микросервисов на базе Eureka
  - Порт: `8761`
  - URL: http://localhost:8761

- **Config Server** (`config-server`) — централизованный сервис конфигурации
  - Динамический порт (Eureka)
  - Хранит конфигурации всех микросервисов

- **Gateway Server** (`gateway-server`) — API Gateway на базе Spring Cloud Gateway
  - Порт: `8080`
  - Единая точка входа для всех клиентских запросов
  - URL: http://localhost:8080

### Бизнес-сервисы

- **User Service** (`user-service`) — управление пользователями
  - Динамический порт (Eureka)
  - База данных: `ewm_main_db` (PostgreSQL)

- **Event Service** (`event-service`) — управление событиями, категориями и подборками
  - Динамический порт (Eureka)
  - База данных: `ewm_main_db` (PostgreSQL)

- **Request Service** (`request-service`) — управление запросами на участие в событиях
  - Динамический порт (Eureka)
  - База данных: `ewm_main_db` (PostgreSQL)

- **Comment Service** (`comment-service`) — управление комментариями к событиям
  - Динамический порт (Eureka)
  - База данных: `ewm_main_db` (PostgreSQL)

- **Stats Server** (`stats-server`) — сервис статистики просмотров событий
  - Порт: `9090`
  - База данных: `ewm_stats_db` (PostgreSQL)

### Базы данных

- **ewm_main_db** (PostgreSQL) — основная база данных
  - Порт: `5432`
  - Используется сервисами: `user-service`, `event-service`, `request-service`, `comment-service`
  - Инициализация таблиц происходит автоматически через Spring Boot SQL initialization (`schema.sql` в каждом сервисе)

- **ewm_stats_db** (PostgreSQL) — база данных статистики
  - Порт: `6543`
  - Используется сервисом: `stats-server`


### Конфигурации

Все конфигурации сервисов находятся в централизованном хранилище.
Каждый сервис также имеет локальный `application.yaml` в `src/main/resources/`, который содержит:
- Настройки подключения к Config Server
- Настройки подключения к Eureka
- Имя приложения

### Внутренний API (Inter-Service Communication)

Взаимодействие между микросервисами осуществляется через **FeignClient** из модуля `interaction-api`. Все клиенты находятся в пакете `ru.practicum.client` и используют Circuit Breaker для отказоустойчивости.

### Fallback механизмы

Все FeignClient имеют fallback-реализации для обработки ошибок:

### Circuit Breaker

Все FeignClient используют Circuit Breaker (Resilience4j) с настройками:
- `connectTimeout: 5000ms`
- `readTimeout: 5000ms`
- Автоматическое переключение на fallback при ошибках

## Функциональность

### Основные возможности

- **Управление пользователями** — создание, получение, удаление пользователей
- **Управление категориями** — создание, редактирование, удаление категорий событий
- **Управление событиями** — создание, редактирование, публикация, поиск событий
- **Управление запросами** — подача заявок на участие, подтверждение/отклонение заявок
- **Управление подборками** — создание подборок событий, добавление/удаление событий
- **Статистика просмотров** — отслеживание количества просмотров событий
- **Система комментариев** — создание, редактирование, модерация комментариев

### Система комментариев

Для пользователей:
- Создание комментариев к событиям
- Редактирование своих комментариев (только в статусе PENDING)
- Удаление своих комментариев
- Просмотр комментариев к событиям

Для администраторов:
- Модерация комментариев (подтверждение/отклонение)
- Поиск комментариев по различным критериям:
  - Статус (PENDING, CONFIRMED, REJECTED)
  - ID события
  - ID автора
  - Временной диапазон
- Удаление любых комментариев
- Редактирование комментариев

**Статусы комментариев:**
- `PENDING` — ожидает модерации
- `CONFIRMED` — подтвержден администратором
- `REJECTED` — отклонен администратором

## Технологии

- **Backend:** Java 21, Spring Boot 3.3.0
- **Microservices:** Spring Cloud (Gateway, Config Server, Eureka, OpenFeign)
- **Database:** PostgreSQL 16.1
- **ORM:** Spring Data JPA, Hibernate
- **Build Tool:** Maven
- **Containerization:** Docker, Docker Compose
- **Code Quality:** Checkstyle, SpotBugs, JaCoCo
- **Resilience:** Resilience4j (Circuit Breaker)

## Запуск проекта

### Предварительные требования

- Docker и Docker Compose
- Java 21
- Maven 3.6+

### Быстрый старт

```bash
# Запуск баз данных
docker-compose up -d

# Сборка проекта
mvn clean install -DskipTests

# Запуск сервисов (в отдельных терминалах или через IDE):
# 1. Discovery Server
# 2. Config Server
# 3. Gateway Server
# 4. User Service
# 5. Event Service
# 6. Request Service
# 7. Comment Service
# 8. Stats Server
```


### Доступные сервисы

- **Gateway Server:** http://localhost:8080
- **Discovery Server (Eureka):** http://localhost:8761
- **Stats Server:** http://localhost:9090
- **Main DB:** localhost:5432
- **Stats DB:** localhost:6543

## API Endpoints

### Публичный API (через Gateway)

Все запросы к API выполняются через Gateway Server на порту `8080`.

#### События
- `GET /events` — получение списка событий с фильтрацией
- `GET /events/{eventId}` — получение события по ID
- `GET /events/{eventId}/comments` — получение комментариев к событию

#### Категории
- `GET /categories` — получение списка категорий
- `GET /categories/{catId}` — получение категории по ID

#### Подборки
- `GET /compilations` — получение списка подборок
- `GET /compilations/{compId}` — получение подборки по ID

### Приватный API (для авторизованных пользователей)

#### События
- `POST /users/{userId}/events` — создание события
- `GET /users/{userId}/events` — получение событий пользователя
- `GET /users/{userId}/events/{eventId}` — получение события пользователя
- `PATCH /users/{userId}/events/{eventId}` — обновление события
- `POST /users/{userId}/events/{eventId}` — публикация события

#### Запросы на участие
- `POST /users/{userId}/requests?eventId={eventId}` — создание запроса на участие
- `GET /users/{userId}/requests` — получение запросов пользователя
- `PATCH /users/{userId}/requests/{requestId}/cancel` — отмена запроса

#### Комментарии
- `POST /users/{userId}/events/{eventId}/comments` — создание комментария
- `PATCH /users/{userId}/comments/{commentId}` — обновление комментария
- `DELETE /users/{userId}/comments/{commentId}` — удаление комментария

### Административный API

#### Пользователи
- `POST /admin/users` — создание пользователя
- `GET /admin/users` — получение списка пользователей
- `GET /admin/users/{userId}` — получение пользователя по ID
- `DELETE /admin/users/{userId}` — удаление пользователя

#### Категории
- `POST /admin/categories` — создание категории
- `PATCH /admin/categories/{catId}` — обновление категории
- `DELETE /admin/categories/{catId}` — удаление категории

#### События
- `GET /admin/events` — поиск событий
- `PATCH /admin/events/{eventId}` — обновление события
- `PATCH /admin/events/{eventId}/publish` — публикация события
- `PATCH /admin/events/{eventId}/reject` — отклонение события

#### Подборки
- `POST /admin/compilations` — создание подборки
- `PATCH /admin/compilations/{compId}` — обновление подборки
- `DELETE /admin/compilations/{compId}` — удаление подборки

#### Комментарии
- `GET /admin/comments` — поиск комментариев (с фильтрами)
- `PATCH /admin/comments/{commentId}` — модерация комментария
- `DELETE /admin/comments/{commentId}` — удаление комментария

### Основные эндпоинты

Все запросы выполняются через Gateway Server (`http://localhost:8080`):

- **Публичный API:** `/events`, `/categories`, `/compilations`, `/events/{eventId}/comments`
- **Приватный API:** `/users/{userId}/events`, `/users/{userId}/requests`, `/users/{userId}/comments`
- **Административный API:** `/admin/users`, `/admin/categories`, `/admin/events`, `/admin/compilations`, `/admin/comments`, `/admin/requests`


## Особенности архитектуры

1. **Централизованная конфигурация** — все настройки сервисов хранятся в Config Server
2. **Service Discovery** — автоматическое обнаружение сервисов через Eureka
3. **API Gateway** — единая точка входа для всех клиентских запросов
4. **Circuit Breaker** — отказоустойчивость при взаимодействии между сервисами
5. **Fallback механизмы** — Приложение устойчиво к сбоям в работе сервисов

## Заметки для разработчика

### Добавление нового сервиса

1. Добавить конфигурацию в `infra/config-server/src/main/resources/config/`
2. Зарегистрировать сервис в Eureka
3. Добавить маршрут в Gateway (если нужен внешний доступ)
4. При необходимости добавить FeignClient в `interaction-api`

<br>

_Проект создан в рамках учебного курса YandexPracticum(JavaDeveloper(расширенный курс))._

