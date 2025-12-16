# Spring Boot Backend Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*FULL.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

