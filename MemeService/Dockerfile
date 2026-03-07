FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file
COPY build/libs/MemeService-1.0.0.jar app.jar

# Expose the port the app runs on
EXPOSE 8082

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
