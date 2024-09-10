# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim-bullseye

# Set the working directory in the container
WORKDIR /app

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Copy and run your application
VOLUME /tmp
ARG JAR_FILE=target/doc-chat-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
COPY src/main/resources/application.properties /app/config/application.properties
#COPY src/main/resources/cnx /app/config/cnx
ENTRYPOINT ["java", "-Dspring.config.location=file:/app/config/application.properties", "-jar", "/app/app.jar"]
