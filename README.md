# FinanceManager

В проекте реализованы:
- регистрация и авторизация по логину/паролю (`/auth/register`, `/auth/login`);
- JWT-аутентификация для защищённых API;
- хранение доходов/расходов/категорий пользователей на сервере в БД (PostgreSQL через Spring Data JPA).

## Что нужно для запуска
- Java 21
- PostgreSQL 14+

## 1) Поднимите PostgreSQL

Пример через Docker:

```bash
docker run --name finance-manager-db \
  -e POSTGRES_DB=finance_manager \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16
```

## 2) Настройте переменные окружения

Приложение читает настройки из `src/main/resources/application.properties`:

- `SPRING_DATASOURCE_URL` (по умолчанию `jdbc:postgresql://localhost:5432/finance_manager`)
- `SPRING_DATASOURCE_USERNAME` (по умолчанию `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (по умолчанию `postgres`)
- `JWT_SECRET` (секрет для подписи JWT)
- `FNS_API_TOKEN` (токен API ФНС для сканирования чеков)

Пример:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance_manager
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export FNS_API_TOKEN=your_real_token
```

## 3) Запуск приложения

```bash
./gradlew bootRun
```

После старта UI доступен по адресу:
- `http://localhost:8080`

## 4) Тесты

```bash
./gradlew test --no-daemon
```

Тесты запускаются на встроенной H2 БД (`src/test/resources/application-test.properties`) и не требуют PostgreSQL.

## Нужно ли подключать MongoDB?

Для текущего функционала MongoDB не нужна: все данные пользователей и трат уже хранятся в PostgreSQL.
