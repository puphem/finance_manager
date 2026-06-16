# Сборка
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .

# Даём права на выполнение gradlew
RUN chmod +x gradlew

# Увеличиваем память для Gradle (чтобы избежать OutOfMemoryError)
ENV GRADLE_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=512m"

# Сборка проекта (пропускаем тесты для скорости)
RUN ./gradlew build -x test --no-daemon

# Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]