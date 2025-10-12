# Multi-stage build for production optimization
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app
COPY pom.xml .
COPY src src

# Download dependencies
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn dependency:go-offline -B

# Build application
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim

# Security: Create non-root user
RUN groupadd -r fileservice && useradd -r -g fileservice fileservice

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application jar
COPY --from=builder /app/target/FileStorageService-*.jar app.jar

# Copy configuration files
COPY --chown=fileservice:fileservice src/main/resources/application-prod.yml application-prod.yml

# Create log directory
RUN mkdir -p /var/log/file-storage-service && \
    chown -R fileservice:fileservice /var/log/file-storage-service

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# Switch to non-root user
USER fileservice

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dspring.profiles.active=production"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
