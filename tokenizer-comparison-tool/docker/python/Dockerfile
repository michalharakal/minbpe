FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy CLI script
COPY cli.py .

# Make CLI script executable
RUN chmod +x cli.py

# Create shared data directory
RUN mkdir -p /shared

# Set entrypoint
ENTRYPOINT ["python", "cli.py"]