# Сборка приложения
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Запуск приложения
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/TelegramTaskBot-0.0.1-SNAPSHOT.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]