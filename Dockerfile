# Сборка
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

# Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]