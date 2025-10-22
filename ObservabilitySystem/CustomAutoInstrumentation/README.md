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

#### Single Python Version

```bash
cd CustomAutoInstrumentation
docker build -t custom-otel-autoinstrumentation-python:1.0.0 .
```

#### Multiple Python Versions

The auto-instrumentation now supports multiple Python versions with version-specific dependencies. Use the provided build script:

```bash
cd CustomAutoInstrumentation
chmod +x build-multi-version.sh
./build-multi-version.sh
```

This will build images for Python 3.8, 3.9, 3.10, 3.11, and 3.12 by default, each with appropriate dependency versions. You can specify custom versions:

```bash
./build-multi-version.sh --versions 3.8,3.9,3.10
```

#### Offline Building and Connection Issues

If you encounter Docker connection issues or need to build in an offline environment:

```bash
# Build using locally available images only (no network access)
./build-multi-version.sh --offline

# Specify an alternative Docker registry
./build-multi-version.sh --registry mirror.example.com

# Configure retry behavior for network operations
./build-multi-version.sh --retries 5 --retry-delay 10
```

For all available options:

```bash
./build-multi-version.sh --help
```

##### Version-Specific Requirements

Each Python version uses its own requirements file:
- Python 3.8: `requirements-3.8.txt`
- Python 3.9: `requirements-3.9.txt`
- Python 3.10: `requirements-3.10.txt`
- Python 3.11: `requirements-3.11.txt`
- Python 3.12: `requirements-3.12.txt`

To add or modify dependencies for a specific Python version, edit the corresponding requirements file.

### Deploying in Kubernetes

1. Install the OpenTelemetry Operator:

```bash
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

2. Apply the custom instrumentation configuration:

```bash
kubectl apply -f custom-instrumentation.yaml
```

3. Annotate your application pods with Python version:

```bash
# For Python 3.8
kubectl annotate pod/your-app-pod instrumentation.opentelemetry.io/python-version=3.8

# For Python 3.9
kubectl annotate pod/your-app-pod instrumentation.opentelemetry.io/python-version=3.9
```

You can also add this annotation in your deployment YAML:

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