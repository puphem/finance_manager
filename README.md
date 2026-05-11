# Как запустить проект (очень просто)

Ниже шаги для человека без опыта. Делай по порядку.

## Что нужно установить
1. **Docker Desktop** (или Docker Engine)
2. **Java 21**

---

## Шаг 1. Поднять базу данных одной командой

Открой терминал в корневой папке проекта (там, где лежат файлы `build.gradle` и `docker-compose.yml`).

Выполни:

```bash
docker compose up -d
```

Готово. База PostgreSQL запущена в фоне.

---

## Шаг 2. Указать переменные окружения

В этом же терминале выполни:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance_manager
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export FNS_API_TOKEN=test-token
```

Если у тебя есть реальный токен ФНС — подставь его вместо `test-token`.

---

## Шаг 3. Запустить приложение

```bash
./gradlew bootRun
```

Когда увидишь, что приложение запустилось, открой в браузере:

`http://localhost:8080`

---

## Шаг 4. Остановить базу (когда закончишь)

```bash
docker compose down
```

---

## Полезно знать
- Регистрация и вход уже работают по логину/паролю.
- Все траты/доходы хранятся на сервере в PostgreSQL, а не локально в браузере.
- MongoDB для текущей версии проекта не нужна.

---

## Проверка тестов

```bash
./gradlew test --no-daemon
```
