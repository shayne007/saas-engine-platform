# Custom OpenTelemetry Auto-Instrumentation

A comprehensive OpenTelemetry auto-instrumentation solution for Python applications, providing enhanced observability with custom telemetry processing capabilities. This module allows you to gain deep insights into your application's performance and behavior without modifying your application code.

## 🚀 Features

- **Custom span processor** for enhanced telemetry data enrichment and filtering
- **Multi-Python version support** (3.8 through 3.12)
- **Comprehensive span attribute processing** for improved trace visualization

## 🛠️ Getting Started
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

## 📊 Implementation Details

### Core Components

1. **Core Module Structure**
   - Directory structure with extensions and tests folders
   - `requirements.txt` with all necessary OpenTelemetry dependencies
   - `custom_configurator.py` for OpenTelemetry configuration
   - `custom_span_processor.py` for custom telemetry processing

2. **Kubernetes Integration**
   - `custom-instrumentation.yaml` with the OpenTelemetry Instrumentation CRD
   - Configuration for resource attributes, sampling, and propagation settings

3. **Testing Components**
   - Flask test application (`test_app.py`)
   - Kubernetes deployment files for testing
   - Dockerfile for the test application

### Custom Span Processor

The custom span processor (`extensions/custom_span_processor.py`) enhances telemetry data by:

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

### Custom Configurator

The `custom_configurator.py` file provides:
- Centralized configuration for OpenTelemetry
- Integration with the custom span processor
- Support for various exporters (OTLP, Prometheus)
- Dynamic resource attribute configuration