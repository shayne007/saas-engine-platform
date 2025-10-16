# Use specific version for reproducibility
FROM python:3.11-slim

# Label for identification
LABEL org.opencontainers.image.source="https://github.com/saas-engine-platform/custom-otel"
LABEL org.opencontainers.image.description="Custom OpenTelemetry Python Auto-Instrumentation"
LABEL version="1.0.0"

# Set working directory
WORKDIR /operator-build

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install OpenTelemetry packages
RUN pip install --no-cache-dir \
    --upgrade pip setuptools wheel && \
    pip install --no-cache-dir -r requirements.txt

# Copy custom extensions
COPY extensions/ /usr/local/lib/python3.11/site-packages/custom_extensions/
COPY custom_configurator.py /usr/local/lib/python3.11/site-packages/opentelemetry/instrumentation/auto_instrumentation/

# Create instrumentation directory
RUN mkdir -p /otel-auto-instrumentation-python

# Copy all installed packages
RUN cp -r /usr/local/lib/python3.11/site-packages/* /otel-auto-instrumentation-python/ && \
    cp /usr/local/bin/opentelemetry-instrument /otel-auto-instrumentation-python/ && \
    cp /usr/local/bin/opentelemetry-bootstrap /otel-auto-instrumentation-python/

# Set permissions
RUN chmod -R 755 /otel-auto-instrumentation-python

# Create version file
RUN echo "1.0.0" > /otel-auto-instrumentation-python/version.txt

WORKDIR /otel-auto-instrumentation-python

# Command to copy files to shared volume
CMD ["cp", "-a", "/otel-auto-instrumentation-python/.", "/otel-auto-instrumentation"]