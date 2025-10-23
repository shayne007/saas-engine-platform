# Multi-version Python auto-instrumentation
ARG PYTHON_VERSION=3.11
FROM python:${PYTHON_VERSION}-slim

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

# Copy all requirements files for different Python versions
COPY requirements*.txt ./

# Install OpenTelemetry packages based on Python version
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    if [ -f requirements-${PYTHON_VERSION}.txt ]; then \
        echo "Using Python ${PYTHON_VERSION} specific requirements" && \
        pip install --no-cache-dir -r requirements-${PYTHON_VERSION}.txt; \
    else \
        echo "No version-specific requirements found, using default" && \
        pip install --no-cache-dir -r requirements.txt; \
    fi

# Get Python version for path
RUN PYTHON_VERSION_PATH=$(python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')") && \
    echo "export PYTHON_VERSION_PATH=$PYTHON_VERSION_PATH" > /env_vars.sh

# Source environment variables and copy custom extensions
RUN . /env_vars.sh && \
    mkdir -p /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/custom_extensions/

# Copy custom extensions to temporary location
COPY extensions/ /tmp/custom_extensions/
COPY custom_configurator.py /tmp/custom_configurator.py

# Move files into Python site-packages based on computed version
RUN PYTHON_VERSION_PATH=$(python -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")') && \
    mkdir -p /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/custom_extensions/ && \
    cp -r /tmp/custom_extensions/* /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/custom_extensions/ && \
    mkdir -p /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/opentelemetry/instrumentation/auto_instrumentation/ && \
    cp /tmp/custom_configurator.py /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/opentelemetry/instrumentation/auto_instrumentation/

# Create instrumentation directory
RUN mkdir -p /otel-auto-instrumentation-python

# Copy all installed packages
RUN PYTHON_VERSION_PATH=$(python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')") && \
    cp -r /usr/local/lib/python${PYTHON_VERSION_PATH}/site-packages/* /otel-auto-instrumentation-python/ && \
    cp /usr/local/bin/opentelemetry-instrument /otel-auto-instrumentation-python/ && \
    cp /usr/local/bin/opentelemetry-bootstrap /otel-auto-instrumentation-python/

# Set permissions
RUN chmod -R 755 /otel-auto-instrumentation-python

# Create version file and Python version file
RUN echo "1.0.0" > /otel-auto-instrumentation-python/version.txt && \
    python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')" > /otel-auto-instrumentation-python/python_version.txt

WORKDIR /otel-auto-instrumentation-python

# Command to copy files to shared volume
CMD ["cp", "-a", "/otel-auto-instrumentation-python/.", "/otel-auto-instrumentation"]