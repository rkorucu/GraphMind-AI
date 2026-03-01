# Stage 1: Build the backend using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the compiled jar from builder
COPY --from=builder /app/target/*.jar aegis-backend.jar

# Expose the correct port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "aegis-backend.jar"]
