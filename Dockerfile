# Build stage con Maven e Java 17
FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage con base Playwright ufficiale (Debian)
FROM mcr.microsoft.com/playwright:focal

# Installa Java 17 OpenJDK
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk && \
    apt-get clean

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

# Forza l’installazione dei browser e dipendenze durante la build
RUN npx playwright install --with-deps

# Copia il jar buildato dal build stage
COPY --from=build /app/target/stripe-checkout-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
