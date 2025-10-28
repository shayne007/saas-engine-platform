FROM python:3.12-slim

WORKDIR /app

# Install Flask and OpenTelemetry dependencies
RUN pip install --no-cache-dir wrapt==1.15.0 \
    flask==2.3.3 \
    opentelemetry-api==1.21.0 \
    opentelemetry-sdk==1.21.0 \
    opentelemetry-instrumentation==0.42b0 \
    opentelemetry-distro==0.42b0 \
    opentelemetry-exporter-otlp-proto-grpc==1.21.0 \
    opentelemetry-exporter-otlp-proto-http==1.21.0 \
    opentelemetry-exporter-prometheus==0.42b0 \
    opentelemetry-instrumentation-redis==0.42b0 \
    opentelemetry-instrumentation-flask==0.42b0 \
    opentelemetry-instrumentation-django==0.42b0 \
    opentelemetry-instrumentation-fastapi==0.42b0 \
    opentelemetry-instrumentation-requests==0.42b0 \
    opentelemetry-instrumentation-logging==0.42b0 \
    opentelemetry-semantic-conventions==0.42b0 \
    opentelemetry-propagator-b3==1.21.0 \  
    opentelemetry-propagator-jaeger==1.21.0

# Copy test application
COPY test_app.py .

# Expose port
EXPOSE 8080

# Run the application
CMD ["python", "test_app.py"]
                                                                                                                                                               