# Fase di build
FROM maven:3.8.3-openjdk-17 AS build
LABEL authors="emanuelromeo"
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Fase di runtime
FROM eclipse-temurin:17-jdk-alpine
LABEL authors="emanuelromeo"
COPY --from=build /app/target/stripe-checkout-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
