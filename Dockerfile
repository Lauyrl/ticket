# ===== Build stage =====
# Use JDK 25 to compile the Spring Boot application
FROM eclipse-temurin:25-jdk AS build 

# Set the working directory of the image
WORKDIR /app

# Copy Maven wrapper and project configuration
COPY .mvn .mvn
COPY mvnw ./
COPY pom.xml ./

# Copy application source code
COPY src src

# Build the JAR without running tests
RUN ./mvnw clean package -DskipTests


# ===== Runtime stage =====
# Use the JRE to run the application
FROM eclipse-temurin:25-jre

# Set the working directory of the image
WORKDIR /app

# Copy the JAR produced by the build stage
COPY --from=build /app/target/*.jar app.jar

# Document that the application listens on port 8080
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
