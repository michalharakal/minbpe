FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src/ src/

# Make gradlew executable
RUN chmod +x gradlew

# Create shared data directory
RUN mkdir -p /shared

# The minbpe-kmp library will be available via shared volume mount
# Build will happen at runtime when the library is available

# Set entrypoint to build and run the CLI
ENTRYPOINT ["./gradlew", "run", "--no-daemon", "--args"]