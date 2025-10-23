# Custom OpenTelemetry Auto-Instrumentation

A comprehensive OpenTelemetry auto-instrumentation solution for Python applications, providing enhanced observability with custom telemetry processing capabilities. This module allows you to gain deep insights into your application's performance and behavior without modifying your application code.

## 🚀 Features

- **Zero-code-change observability** for Python applications
- **Custom span processor** for enhanced telemetry data enrichment and filtering
- **Seamless integration** with OpenTelemetry Collector and Jaeger
- **Multi-Python version support** (3.8 through 3.12)
- **Kubernetes-native deployment** via OpenTelemetry Operator
- **Docker Compose setup** for local development and testing
- **Comprehensive span attribute processing** for improved trace visualization

## 📁 Directory Structure

```
CustomAutoInstrumentation/
├── Dockerfile                # Container image definition
├── docker-compose.yml        # Local development environment configuration
├── collector-config.yaml     # OpenTelemetry Collector configuration
├── requirements.txt          # Python dependencies
├── requirements-*.txt        # Version-specific dependencies
├── custom_configurator.py    # Custom configuration logic
├── extensions/               # Custom extensions
│   ├── __init__.py
│   └── custom_span_processor.py  # Enhanced span processing logic
├── tests/                    # Test applications and scripts
│   └── test_app.py           # Example Flask application for testing
├── custom-instrumentation.yaml  # Kubernetes configuration
└── README.md                 # Documentation
```

## 🛠️ Getting Started

### Local Development with Docker Compose

The project includes a Docker Compose setup for easy local development and testing:

```bash
cd CustomAutoInstrumentation
docker-compose up -d
```

This will start the following services:
- `test-app`: Example Flask application with instrumentation
- `otel-collector`: OpenTelemetry Collector for processing telemetry data
- `jaeger`: Jaeger UI for visualizing traces

Access the services:
- Jaeger UI: http://localhost:16686
- Test Application:
  - Manual span endpoint: http://localhost:8080/manual-span
  - Slow operation endpoint: http://localhost:8080/slow

### Building the Custom Image

#### Single Python Version

```bash
cd CustomAutoInstrumentation
docker build -t custom-otel-autoinstrumentation-python:1.0.0 .
```

#### Multiple Python Versions

The auto-instrumentation supports multiple Python versions with version-specific dependencies. Use the provided build script:

```bash
cd CustomAutoInstrumentation
chmod +x build-multi-version.sh
./build-multi-version.sh
```

This will build images for Python 3.8, 3.9, 3.10, 3.11, and 3.12 by default. You can specify custom versions:

```bash
./build-multi-version.sh --versions 3.8,3.9,3.10
```

#### Advanced Build Options

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

#### Version-Specific Requirements

Each Python version uses its own requirements file:
- Python 3.8: `requirements-3.8.txt`
- Python 3.9: `requirements-3.9.txt`
- Python 3.10: `requirements-3.10.txt`
- Python 3.11: `requirements-3.11.txt`
- Python 3.12: `requirements-3.12.txt`

## 📦 Deploying in Kubernetes

### Prerequisites
- Kubernetes cluster (v1.16+)
- kubectl configured with cluster access

### Deployment Steps

1. Install the OpenTelemetry Operator:

```bash
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

2. Apply the custom instrumentation configuration:

```bash
kubectl apply -f custom-instrumentation.yaml
```

3. Annotate your application pods for auto-instrumentation:

```bash
# For Python 3.8
kubectl annotate pod/your-app-pod instrumentation.opentelemetry.io/python-version=3.8

# For Python 3.9
kubectl annotate pod/your-app-pod instrumentation.opentelemetry.io/python-version=3.9
```

Alternatively, add annotations directly in your deployment YAML:

```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
```

## ⚙️ Configuration

### Environment Variables

The custom auto-instrumentation can be configured using these key environment variables:

| Environment Variable | Description | Example |
|----------------------|-------------|--------|
| `OTEL_SERVICE_NAME` | Name of the service | `my-python-app` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Endpoint for the OpenTelemetry Collector | `http://otel-collector:4317` |
| `DEPLOYMENT_ENVIRONMENT` | Environment name | `development`, `staging`, `production` |
| `OTEL_PYTHON_DISABLED_INSTRUMENTATIONS` | Comma-separated list of instrumentations to disable | `flask,django` |
| `OTEL_TRACES_EXPORTER` | Trace exporter to use | `otlp` |
| `OTEL_LOG_LEVEL` | Log level for the OpenTelemetry SDK | `INFO`, `DEBUG`, `ERROR` |

### OpenTelemetry Collector Configuration

The `collector-config.yaml` file configures how telemetry data is processed and exported:

- **Receivers**: OTLP gRPC and HTTP endpoints
- **Processors**: Batching, memory limiting, and resource enrichment
- **Exporters**: Jaeger, Prometheus metrics, and debug logging
- **Pipelines**: Separate pipelines for traces, metrics, and logs

## 🔍 Custom Span Processor

The `custom_span_processor.py` provides enhanced telemetry data processing capabilities:

### Key Features

- **Span Attribute Enrichment**:
  - Adds `custom.processed` and `custom.processor.version` attributes to all spans
  - Adds `custom.slow_operation: true` to spans that took longer than 100ms
  - Adds `custom.error_processed: true` to spans with error status
  - Preserves and enhances HTTP request/response attributes

- **Advanced Filtering**:
  - Intelligent span filtering based on operation names and attributes
  - Error detection and special handling for problematic spans

- **JSON Serialization Support**:
  - Proper handling of complex data types including enums and status codes
  - Comprehensive logging for span processing and troubleshooting

### Recent Improvements

- Fixed JSON serialization issues with StatusCode enum types
- Optimized span data handling for better performance
- Enhanced error handling and logging

## 🧪 Testing

The project includes a test application for validating the instrumentation:

```bash
# Start the test environment
docker-compose up -d

# Test manual span creation
curl http://localhost:8080/manual-span

# Test slow operation span
curl http://localhost:8080/slow

# Check logs for span processing
docker logs customautoinstrumentation-test-app-1 | grep -E 'custom_span_processor'
```

## 📊 Viewing Traces in Jaeger

1. Access the Jaeger UI at http://localhost:16686
2. Select your service from the dropdown menu
3. Click "Find Traces" to view recent traces
4. Explore individual traces to see enriched span attributes

## ❓ Troubleshooting

### Common Issues

1. **Span attributes not appearing in Jaeger**
   - Check if the OpenTelemetry Collector is properly configured
   - Verify the connection between your application, collector, and Jaeger
   - Ensure your `collector-config.yaml` has the correct exporters configured

2. **JSON serialization errors**
   - Recent fixes address StatusCode enum serialization issues
   - Ensure you're using the latest version of the custom span processor

3. **Docker Compose issues**
   - Use `docker-compose down` followed by `docker-compose up -d` to fully restart services
   - Check container logs for specific error messages

## 📝 License

MIT License - see the LICENSE file for details