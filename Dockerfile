FROM maven:3-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17.0.2-jdk-oracle
WORKDIR /app
COPY --from=builder /app/target/TelegramTaskBot-*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]