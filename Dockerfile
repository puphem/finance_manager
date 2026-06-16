# Сборка с кэшированием Gradle-зависимостей
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradle/ gradle/
COPY gradlew .
RUN ./gradlew dependencies --no-daemon   # скачает зависимости
COPY src/ src/
RUN ./gradlew build -x test --no-daemon

# Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]