# Custom OpenTelemetry Auto-Instrumentation

This module provides a custom OpenTelemetry auto-instrumentation solution for Python applications running in Kubernetes.

## Features

- Zero-code-change observability for Python applications
- Custom span processor for enhanced telemetry data
- Seamless integration with OpenTelemetry Collector
- Kubernetes-native deployment via OpenTelemetry Operator

## Directory Structure

```
CustomAutoInstrumentation/
├── Dockerfile                # Container image definition
├── requirements.txt          # Python dependencies
├── custom_configurator.py    # Custom configuration logic
├── extensions/               # Custom extensions
│   ├── __init__.py
│   └── custom_span_processor.py
├── custom-instrumentation.yaml  # Kubernetes configuration
└── README.md                 # Documentation
```

## Usage

### Building the Custom Image

```bash
cd CustomAutoInstrumentation
docker build -t custom-otel-autoinstrumentation-python:1.0.0 .
```

### Deploying in Kubernetes

1. Install the OpenTelemetry Operator:

```bash
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

2. Apply the custom instrumentation configuration:

```bash
kubectl apply -f custom-instrumentation.yaml
```

3. Annotate your application pods:

```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
```

## Configuration

The custom auto-instrumentation can be configured using environment variables:

- `OTEL_SERVICE_NAME`: Name of the service
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Endpoint for the OpenTelemetry Collector
- `DEPLOYMENT_ENVIRONMENT`: Environment name (development, staging, production)
- `OTEL_PYTHON_DISABLED_INSTRUMENTATIONS`: Comma-separated list of instrumentations to disable

## Custom Span Processor

The custom span processor adds additional attributes to spans and provides custom filtering capabilities:

- Adds `custom.processed` and `custom.processor.version` attributes to all spans
- Adds `custom.slow_operation: true` to spans that took longer than 100ms
- Adds `custom.error_processed: true` to spans with error status

## License

MIT