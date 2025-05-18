FROM maven AS builder
COPY ./ ./
RUN mvn clean package -DskipTests
FROM openjdk:21-oracle
COPY --from=builder /target/TelegramTaskBot-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]