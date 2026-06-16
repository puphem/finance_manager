# Первый этап: сборка приложения с помощью Gradle
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Второй этап: запуск готового JAR-файла
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]