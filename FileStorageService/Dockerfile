# Multi-stage build for APIGateway

# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy gradle files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/

# Make gradlew executable and fix line endings (for Windows compatibility)
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
COPY build.gradle settings.gradle ./

# Copy source code
COPY src/ src/

# Build the application (skip tests for faster build)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN addgroup --system memeverse && adduser --system memeverse --ingroup memeverse

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/FileStorageService-1.0.0.jar FileStorageService.jar

# Set ownership
RUN chown memeverse:memeverse FileStorageService.jar

USER memeverse:memeverse

## Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
CMD ["java", "-jar", "FileStorageService.jar"]
