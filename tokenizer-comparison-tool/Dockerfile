FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install system dependencies including Docker CLI
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source code
COPY src/ src/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build --no-daemon

# Create directories for shared data and reports
RUN mkdir -p /shared /app/reports /app/test-corpus

# Set entrypoint to run the comparison tool
ENTRYPOINT ["./gradlew", "run", "--no-daemon", "--args"]