# Step 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source
COPY src ./src

# Build
RUN mvn clean package -DskipTests


# Step 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/trekking-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

# JVM optimized for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]