# Base image
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the jar file
COPY target/retrivium-1.0-SNAPSHOT.jar /app/retrivium-1.0-SNAPSHOT.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "retrivium-1.0-SNAPSHOT.jar"]

# Set the default command
CMD ["--help"]
