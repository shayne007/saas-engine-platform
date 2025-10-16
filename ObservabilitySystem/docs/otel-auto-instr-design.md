# OpenTelemetry Auto-Instrumentation Design Document

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Injection Mechanism](#injection-mechanism)
5. [Language-Specific Implementations](#language-specific-implementations)
6. [Configuration Management](#configuration-management)
7. [Data Flow](#data-flow)
8. [Custom Image Development](#custom-image-development)
9. [Deployment Patterns](#deployment-patterns)
10. [Security Considerations](#security-considerations)
11. [Performance Impact](#performance-impact)
12. [Best Practices](#best-practices)

---

## Overview

### Purpose
OpenTelemetry Operator for Auto-Instrumentation provides zero-code-change observability for Kubernetes applications by automatically injecting OpenTelemetry SDK libraries and configuration at runtime.

### Goals
- Enable observability without modifying application code
- Provide centralized instrumentation configuration
- Support multiple programming languages
- Integrate seamlessly with Kubernetes workflows
- Minimize operational overhead

### Key Features
- Automatic SDK injection via Kubernetes admission webhooks
- Support for Java, Python, Node.js, .NET, Go, and Apache HTTPD
- Integration with OpenTelemetry Collector
- Kubernetes-native configuration via CRDs
- Automatic resource attribute injection

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         OpenTelemetry Operator                     │    │
│  │  ┌──────────────┐  ┌────────────────────────┐    │    │
│  │  │ Instrumentation│  │   Collector            │    │    │
│  │  │   Controller   │  │   Controller           │    │    │
│  │  └──────┬─────────┘  └────────────────────────┘    │    │
│  │         │                                           │    │
│  │  ┌──────▼──────────────────────────────────┐      │    │
│  │  │   Mutating Admission Webhook            │      │    │
│  │  └──────┬──────────────────────────────────┘      │    │
│  └─────────┼──────────────────────────────────────────┘    │
│            │                                                │
│            │ Intercepts Pod Creation                       │
│            │                                                │
│  ┌─────────▼──────────────────────────────────────────┐   │
│  │              Application Pod                        │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │  Init Container (Copy Instrumentation)       │  │   │
│  │  └───────────────┬──────────────────────────────┘  │   │
│  │                  │ Shared Volume                    │   │
│  │  ┌───────────────▼──────────────────────────────┐  │   │
│  │  │  Application Container                       │  │   │
│  │  │  - Injected env vars                         │  │   │
│  │  │  - OTel SDK loaded                           │  │   │
│  │  │  - Auto-instrumentation active               │  │   │
│  │  └───────────────┬──────────────────────────────┘  │   │
│  └──────────────────┼─────────────────────────────────┘   │
│                     │ Telemetry Data (OTLP)               │
│                     │                                      │
│  ┌──────────────────▼─────────────────────────────────┐   │
│  │     OpenTelemetry Collector                        │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │ Receivers → Processors → Exporters           │  │   │
│  │  └───────────────┬──────────────────────────────┘  │   │
│  └──────────────────┼─────────────────────────────────┘   │
└────────────────────┼──────────────────────────────────────┘
                     │
                     ▼
          Backend (Jaeger, Prometheus, etc.)
```

### Component Interaction Flow

```
User Creates Pod with Annotation
         │
         ▼
Mutating Admission Webhook Intercepts
         │
         ▼
Reads Instrumentation CRD Configuration
         │
         ▼
Modifies Pod Specification:
  - Adds Init Container
  - Injects Environment Variables
  - Adds Volume Mounts
  - Wraps Application Command (if needed)
         │
         ▼
Pod Starts:
  1. Init Container Copies SDK to Shared Volume
  2. Application Container Starts
  3. SDK Auto-Instruments Application
  4. Telemetry Flows to Collector
```

---

## Core Components

### 1. OpenTelemetry Operator

**Responsibilities:**
- Manages lifecycle of Instrumentation and Collector CRDs
- Watches for pod creation events
- Coordinates webhook and controller operations

**Installation:**
```bash
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

### 2. Instrumentation Controller

**Purpose:** Manages Instrumentation custom resources

**Key Functions:**
- Validates Instrumentation CRD configurations
- Provides default values for missing configurations
- Updates status of Instrumentation resources
- Handles language-specific image management

### 3. Mutating Admission Webhook

**Purpose:** Intercepts and modifies pod creation requests

**Trigger Conditions:**
- Pod has instrumentation annotation
- Namespace not excluded
- Pod not already instrumented

**Modifications Applied:**
```yaml
# Original Pod
apiVersion: v1
kind: Pod
metadata:
  name: my-app
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
spec:
  containers:
  - name: app
    image: my-app:latest

# Modified Pod (simplified)
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  initContainers:
  - name: opentelemetry-auto-instrumentation
    image: autoinstrumentation-python:latest
    command: [cp, -r, /autoinstrumentation/., /otel-auto-instrumentation]
    volumeMounts:
    - name: opentelemetry-auto-instrumentation
      mountPath: /otel-auto-instrumentation
  containers:
  - name: app
    image: my-app:latest
    env:
    - name: PYTHONPATH
      value: /otel-auto-instrumentation/opentelemetry/instrumentation/auto_instrumentation
    - name: OTEL_SERVICE_NAME
      value: my-app
    - name: OTEL_EXPORTER_OTLP_ENDPOINT
      value: http://otel-collector:4317
    volumeMounts:
    - name: opentelemetry-auto-instrumentation
      mountPath: /otel-auto-instrumentation
  volumes:
  - name: opentelemetry-auto-instrumentation
    emptyDir: {}
```

### 4. Collector Controller

**Purpose:** Manages OpenTelemetry Collector deployments

**Deployment Modes:**
- **DaemonSet:** Collector on every node (recommended for auto-instrumentation)
- **Deployment:** Centralized collector instances
- **StatefulSet:** For persistent storage scenarios
- **Sidecar:** Per-pod collector instances

---

## Injection Mechanism

### Webhook Registration

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: opentelemetry-operator-mutation
webhooks:
- name: mpod.kb.io
  admissionReviewVersions: ["v1"]
  sideEffects: None
  clientConfig:
    service:
      name: opentelemetry-operator-webhook
      namespace: opentelemetry-operator-system
      path: /mutate-v1-pod
  rules:
  - operations: ["CREATE", "UPDATE"]
    apiGroups: [""]
    apiVersions: ["v1"]
    resources: ["pods"]
```

### Injection Decision Tree

```
Pod Creation Request
    │
    ├─ Has instrumentation annotation? ─NO─> Allow unchanged
    │   YES
    │
    ├─ Namespace excluded? ─YES─> Allow unchanged
    │   NO
    │
    ├─ Already instrumented? ─YES─> Allow unchanged
    │   NO
    │
    ├─ Valid Instrumentation resource exists? ─NO─> Reject with error
    │   YES
    │
    ├─ Language supported? ─NO─> Reject with error
    │   YES
    │
    └─> Apply Instrumentation Modifications
```

### Volume and Mount Strategy

**Shared Volume Approach:**
```yaml
# Init container writes to volume
initContainers:
- name: opentelemetry-auto-instrumentation-python
  volumeMounts:
  - name: opentelemetry-auto-instrumentation-python
    mountPath: /otel-auto-instrumentation

# App container reads from volume
containers:
- name: app
  volumeMounts:
  - name: opentelemetry-auto-instrumentation-python
    mountPath: /otel-auto-instrumentation

volumes:
- name: opentelemetry-auto-instrumentation-python
  emptyDir:
    sizeLimit: 500Mi  # Adjust based on SDK size
```

---

## Language-Specific Implementations

### Java Auto-Instrumentation

**Mechanism:** Java Agent bytecode manipulation

**Injection Process:**
1. Init container copies Java agent JAR to shared volume
2. JVM argument injected: `-javaagent:/otel-auto-instrumentation/javaagent.jar`
3. Agent loads at JVM startup
4. Bytecode instrumentation applied at class load time

**Environment Variables:**
```yaml
env:
- name: JAVA_TOOL_OPTIONS
  value: "-javaagent:/otel-auto-instrumentation/javaagent.jar"
- name: OTEL_SERVICE_NAME
  value: "my-java-app"
- name: OTEL_EXPORTER_OTLP_ENDPOINT
  value: "http://otel-collector:4317"
- name: OTEL_RESOURCE_ATTRIBUTES
  value: "service.version=1.0,deployment.environment=prod"
```

**Supported Frameworks:**
- Spring Boot, Spring WebMVC
- Jakarta EE, Java EE
- JDBC, JPA, Hibernate
- Apache HttpClient, OkHttp
- Kafka, RabbitMQ, JMS
- Netty, Vert.x

### Python Auto-Instrumentation

**Mechanism:** Python import hooks and wrapper functions

**Injection Process:**
1. Init container copies OpenTelemetry packages to shared volume
2. PYTHONPATH modified to include instrumentation packages
3. Application command wrapped with `opentelemetry-instrument`
4. Import hooks intercept library imports and apply instrumentation

**Environment Variables:**
```yaml
env:
- name: PYTHONPATH
  value: "/otel-auto-instrumentation/opentelemetry/instrumentation/auto_instrumentation:/otel-auto-instrumentation"
- name: OTEL_TRACES_EXPORTER
  value: "otlp"
- name: OTEL_METRICS_EXPORTER
  value: "otlp"
- name: OTEL_LOGS_EXPORTER
  value: "otlp"
- name: OTEL_PYTHON_LOGGING_AUTO_INSTRUMENTATION_ENABLED
  value: "true"
```

**Command Wrapping:**
```bash
# Original: python app.py
# Modified: opentelemetry-instrument python app.py
```

**Supported Frameworks:**
- Flask, Django, FastAPI
- Requests, HTTPX, AIOHTTP
- SQLAlchemy, Psycopg2, PyMongo
- Celery, Redis, Kafka
- AWS SDK (Boto3)

### Node.js Auto-Instrumentation

**Mechanism:** Require hooks and module patching

**Injection Process:**
1. Init container copies Node.js instrumentation packages
2. NODE_OPTIONS modified to require instrumentation
3. Module loader patched to instrument required modules
4. Automatic instrumentation applied at require time

**Environment Variables:**
```yaml
env:
- name: NODE_PATH
  value: "/otel-auto-instrumentation/node_modules"
- name: NODE_OPTIONS
  value: "--require /otel-auto-instrumentation/autoinstrumentation.js"
- name: OTEL_NODE_RESOURCE_DETECTORS
  value: "env,host,os,serviceinstance"
```

**Supported Frameworks:**
- Express, Koa, Fastify, NestJS
- HTTP, HTTPS modules
- MySQL, PostgreSQL, MongoDB, Redis
- AWS SDK, gRPC
- Kafka, AMQP

### .NET Auto-Instrumentation

**Mechanism:** CLR Profiling API

**Injection Process:**
1. Init container copies .NET profiler and instrumentation assemblies
2. CLR profiler environment variables configured
3. Profiler DLL loaded by CLR at startup
4. IL rewriting applied to instrument methods

**Environment Variables:**
```yaml
env:
- name: CORECLR_ENABLE_PROFILING
  value: "1"
- name: CORECLR_PROFILER
  value: "{918728DD-259F-4A6A-AC2B-B85E1B658318}"
- name: CORECLR_PROFILER_PATH
  value: "/otel-auto-instrumentation/linux-x64/OpenTelemetry.AutoInstrumentation.Native.so"
- name: DOTNET_STARTUP_HOOKS
  value: "/otel-auto-instrumentation/net/OpenTelemetry.AutoInstrumentation.StartupHook.dll"
- name: DOTNET_ADDITIONAL_DEPS
  value: "/otel-auto-instrumentation/AdditionalDeps"
- name: OTEL_DOTNET_AUTO_HOME
  value: "/otel-auto-instrumentation"
```

**Supported Frameworks:**
- ASP.NET Core, ASP.NET
- Entity Framework Core, ADO.NET
- HttpClient, gRPC
- MongoDB, Redis, Elasticsearch
- Azure SDK, AWS SDK

### Go Auto-Instrumentation

**Mechanism:** eBPF-based instrumentation (experimental)

**Injection Process:**
1. eBPF programs loaded into kernel
2. Uprobe/kprobe attach to Go runtime functions
3. Trace context extracted from goroutine stacks
4. Spans created without modifying application binary

**Limitations:**
- Still experimental and limited coverage
- Requires kernel eBPF support
- Only supports HTTP and gRPC currently
- No manual span creation without code changes

**Environment Variables:**
```yaml
env:
- name: OTEL_GO_AUTO_TARGET_EXE
  value: "/app/main"
- name: OTEL_SERVICE_NAME
  value: "my-go-app"
```

---

## Configuration Management

### Instrumentation CRD Structure

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: production-instrumentation
  namespace: default
spec:
  # Global exporter configuration
  exporter:
    endpoint: http://otel-collector.observability.svc.cluster.local:4317
    
  # Propagators for distributed tracing
  propagators:
    - tracecontext   # W3C Trace Context
    - baggage        # W3C Baggage
    - b3             # Zipkin B3
    - b3multi        # Zipkin B3 Multi-Header
    - jaeger         # Jaeger propagation
    - xray           # AWS X-Ray
    - ottrace        # OpenTracing
  
  # Sampling configuration
  sampler:
    type: parentbased_traceidratio
    argument: "0.1"  # Sample 10% of traces
    # Other sampler types:
    # - always_on: Sample everything
    # - always_off: Sample nothing
    # - traceidratio: Percentage-based
    # - parentbased_always_on: Follow parent or always sample
    # - parentbased_always_off: Follow parent or never sample
    # - parentbased_traceidratio: Follow parent or percentage
  
  # Resource attributes (applied to all telemetry)
  resource:
    addK8sUIDAttributes: true  # Add k8s.*.uid attributes
    resourceAttributes:
      deployment.environment: production
      service.namespace: ecommerce
      service.version: ${DEPLOYMENT_VERSION}
  
  # Language-specific configurations
  java:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:2.0.0
    # Resource requirements for init container
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 500m
        memory: 128Mi
    env:
      # Java agent specific configuration
      - name: OTEL_JAVAAGENT_DEBUG
        value: "false"
      - name: OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED
        value: "true"
      - name: OTEL_INSTRUMENTATION_JDBC_STATEMENT_SANITIZER_ENABLED
        value: "true"
      - name: OTEL_JAVAAGENT_EXTENSIONS
        value: "/otel-auto-instrumentation/extensions"
      # Disable specific instrumentations
      - name: OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED
        value: "true"
      - name: OTEL_INSTRUMENTATION_APACHE_HTTPCLIENT_ENABLED
        value: "true"
      - name: OTEL_INSTRUMENTATION_JDBC_ENABLED
        value: "true"
  
  python:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-python:0.42b0
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 200m
        memory: 128Mi
    env:
      - name: OTEL_TRACES_EXPORTER
        value: "otlp"
      - name: OTEL_METRICS_EXPORTER
        value: "otlp"
      - name: OTEL_LOGS_EXPORTER
        value: "otlp"
      - name: OTEL_PYTHON_LOGGING_AUTO_INSTRUMENTATION_ENABLED
        value: "true"
      - name: OTEL_PYTHON_LOG_CORRELATION
        value: "true"
      # Disable specific instrumentations
      - name: OTEL_PYTHON_DISABLED_INSTRUMENTATIONS
        value: "urllib,urllib3"
  
  nodejs:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-nodejs:0.42.0
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 200m
        memory: 128Mi
    env:
      - name: OTEL_TRACES_EXPORTER
        value: "otlp"
      - name: OTEL_METRICS_EXPORTER
        value: "otlp"
      - name: OTEL_LOGS_EXPORTER
        value: "otlp"
      - name: OTEL_NODE_RESOURCE_DETECTORS
        value: "env,host,os,serviceinstance"
  
  dotnet:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-dotnet:1.0.0
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 200m
        memory: 128Mi
    env:
      - name: OTEL_TRACES_EXPORTER
        value: "otlp"
      - name: OTEL_METRICS_EXPORTER
        value: "otlp"
      - name: OTEL_LOGS_EXPORTER
        value: "otlp"
      - name: OTEL_DOTNET_AUTO_TRACES_ENABLED
        value: "true"
      - name: OTEL_DOTNET_AUTO_METRICS_ENABLED
        value: "true"
      - name: OTEL_DOTNET_AUTO_LOGS_ENABLED
        value: "true"
  
  go:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-go:0.6.0-alpha
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 200m
        memory: 128Mi
    env:
      - name: OTEL_GO_AUTO_TARGET_EXE
        value: "/path/to/executable"
```

### Annotation-Based Configuration

**Basic Annotation:**
```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
```

**Reference Specific Instrumentation Resource:**
```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "my-namespace/production-instrumentation"
```

**Multi-Container Pod:**
```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-java: "true"
    instrumentation.opentelemetry.io/container-names: "app,sidecar"
```

**SDK-Only Injection (No Auto-Instrumentation):**
```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-sdk: "true"
```

**Override Service Name:**
```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
  labels:
    app: my-service  # Used as service name if OTEL_SERVICE_NAME not set
```

### Environment Variable Priority

1. **Explicitly set in pod spec** (highest priority)
2. **Injected by operator** from Instrumentation CRD
3. **Default values** from operator configuration
4. **SDK defaults** (lowest priority)

---

## Data Flow

### Telemetry Pipeline

```
Application
    │
    │ (1) Auto-instrumented code generates spans/metrics/logs
    │
    ▼
OpenTelemetry SDK (in-process)
    │
    │ (2) Batches and encodes telemetry data
    │ (3) Adds resource attributes
    │
    ▼
OTLP Exporter
    │
    │ (4) Sends via gRPC/HTTP to collector
    │
    ▼
OpenTelemetry Collector
    │
    ├─ Receivers (OTLP, Jaeger, Zipkin)
    │
    ├─ Processors
    │   ├─ Batch: Batches telemetry for efficiency
    │   ├─ Resource: Adds/modifies resource attributes
    │   ├─ Attributes: Adds/removes span attributes
    │   ├─ Filter: Drops unwanted telemetry
    │   ├─ Tail Sampling: Smart sampling decisions
    │   └─ Memory Limiter: Prevents OOM
    │
    └─ Exporters
        ├─ OTLP (to another collector or backend)
        ├─ Jaeger
        ├─ Prometheus
        ├─ Zipkin
        ├─ Elasticsearch
        ├─ AWS CloudWatch
        └─ Google Cloud Trace
```

### Resource Attribute Flow

```
Instrumentation CRD
    │
    │ resource.resourceAttributes
    │
    ▼
Operator Injection
    │
    ├─ Kubernetes Metadata (auto-detected):
    │   ├─ k8s.namespace.name
    │   ├─ k8s.pod.name
    │   ├─ k8s.pod.uid
    │   ├─ k8s.deployment.name
    │   ├─ k8s.container.name
    │   ├─ k8s.node.name
    │   └─ k8s.cluster.name (if configured)
    │
    ├─ Service Metadata:
    │   ├─ service.name (from label or env)
    │   ├─ service.namespace
    │   └─ service.version
    │
    └─ Custom Attributes (from CRD)
        ├─ deployment.environment
        ├─ team
        └─ cost-center
    │
    ▼
Environment Variables (OTEL_RESOURCE_ATTRIBUTES)
    │
    ▼
Application SDK
    │
    ▼
All Telemetry Data (Traces, Metrics, Logs)
```

### Trace Context Propagation

```
Service A (Instrumented)
    │
    │ (1) Generates trace context
    │     traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
    │
    ▼
HTTP Request Headers
    │
    │ (2) Propagators inject context into headers:
    │     - traceparent (W3C Trace Context)
    │     - tracestate (W3C Trace Context)
    │     - X-B3-TraceId, X-B3-SpanId (B3)
    │     - uber-trace-id (Jaeger)
    │
    ▼
Service B (Instrumented)
    │
    │ (3) Propagators extract context from headers
    │
    ▼
New Span with Parent Context
    │
    │ (4) Continues distributed trace
    │
    ▼
Backend (Unified Trace View)
```

---

## Custom Image Development

### Directory Structure

```
custom-autoinstrumentation/
├── Dockerfile
├── requirements.txt           # Python dependencies
├── custom_configurator.py     # Custom configuration logic
├── extensions/
│   ├── __init__.py
│   └── custom_span_processor.py
├── .dockerignore
└── README.md
```

### Dockerfile Best Practices

```dockerfile
# Use specific version for reproducibility
FROM python:3.11.6-slim

# Label for identification
LABEL org.opencontainers.image.source="https://github.com/yourorg/custom-otel"
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
```

### requirements.txt Example

```txt
# Core OpenTelemetry packages (pinned versions)
opentelemetry-api==1.21.0
opentelemetry-sdk==1.21.0
opentelemetry-instrumentation==0.42b0
opentelemetry-distro==0.42b0

# Exporters
opentelemetry-exporter-otlp-proto-grpc==1.21.0
opentelemetry-exporter-otlp-proto-http==1.21.0
opentelemetry-exporter-prometheus==0.42b0

# Instrumentation libraries
opentelemetry-instrumentation-flask==0.42b0
opentelemetry-instrumentation-django==0.42b0
opentelemetry-instrumentation-fastapi==0.42b0
opentelemetry-instrumentation-requests==0.42b0
opentelemetry-instrumentation-sqlalchemy==0.42b0
opentelemetry-instrumentation-psycopg2==0.42b0
opentelemetry-instrumentation-redis==0.42b0
opentelemetry-instrumentation-celery==0.42b0
opentelemetry-instrumentation-kafka-python==0.42b0

# Additional utilities
opentelemetry-semantic-conventions==0.42b0
opentelemetry-propagator-b3==1.21.0
opentelemetry-propagator-jaeger==1.21.0

# Custom dependencies
your-internal-library==2.3.0
```

### Custom Span Processor Example

```python
# extensions/custom_span_processor.py
from opentelemetry.sdk.trace import SpanProcessor, ReadableSpan
from opentelemetry.sdk.trace.export import SpanExporter
import logging

logger = logging.getLogger(__name__)

class CustomSpanProcessor(SpanProcessor):
    """Custom span processor for filtering or enriching spans."""
    
    def __init__(self, exporter: SpanExporter):
        self.exporter = exporter
        self.dropped_spans = 0
    
    def on_start(self, span: ReadableSpan, parent_context=None):
        """Called when a span is started."""
        # Add custom attributes
        span.set_attribute("custom.processor", "enabled")
        
        # Add business context
        if hasattr(span, "attributes"):
            if "http.url" in span.attributes:
                # Parse and add custom URL attributes
                pass
    
    def on_end(self, span: ReadableSpan):
        """Called when a span is ended."""
        # Filter out health check spans
        if span.name == "/health" or span.name == "/readiness":
            self.dropped_spans += 1
            logger.debug(f"Dropped health check span: {span.name}")
            return
        
        # Filter out fast spans (< 10ms) for high-throughput endpoints
        if span.name.startswith("/api/v1/") and \
           (span.end_time - span.start_time) < 10_000_000:  # 10ms in nanoseconds
            self.dropped_spans += 1
            return
        
        # Export span
        self.exporter.export([span])
    
    def shutdown(self):
        """Called when the processor is shutdown."""
        logger.info(f"Custom processor dropped {self.dropped_spans} spans")
        self.exporter.shutdown()
    
    def force_flush(self, timeout_millis: int = 30000):
        """Force flush any buffered spans."""
        return self.exporter.force_flush(timeout_millis)
```

### Custom Configurator Example

```python
# custom_configurator.py
from opentelemetry.instrumentation.auto_instrumentation.configurator import BaseConfigurator
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from custom_extensions.custom_span_processor import CustomSpanProcessor
import logging

logger = logging.getLogger(__name__)

class CustomConfigurator(BaseConfigurator):
    """Custom configuration for OpenTelemetry auto-instrumentation."""
    
    def _configure(self, **kwargs):
        """Apply custom configuration before instrumentation."""
        logger.info("Applying custom OpenTelemetry configuration")
        
        # Get the tracer provider
        tracer_provider = kwargs.get('tracer_provider')
        
        if tracer_provider:
            # Add custom span processor
            exporter = OTLPSpanExporter()
            custom_processor = CustomSpanProcessor(exporter)
            tracer_provider.add_span_processor(custom_processor)
            
            logger.info("Custom span processor added")
        
        # Add custom resource attributes
        resource_attributes = kwargs.get('resource_attributes', {})
        resource_attributes['custom.version'] = '1.0.0'
        resource_attributes['custom.environment'] = 'production'
        
        return kwargs
```

### Build and Push Script

```bash
#!/bin/bash
# build.sh

set -e

IMAGE_NAME="your-registry.com/autoinstrumentation-python"
VERSION="1.0.0"
PLATFORMS="linux/amd64,linux/arm64"

echo "Building custom auto-instrumentation image..."

# Build multi-platform image
docker buildx build \
  --platform ${PLATFORMS} \
  --tag ${IMAGE_NAME}:${VERSION} \
  --tag ${IMAGE_NAME}:latest \
  --push \
  .

echo "Image pushed: ${IMAGE_NAME}:${VERSION}"

# Scan for vulnerabilities (optional)
docker scout cves ${IMAGE_NAME}:${VERSION}
```

---

## Deployment Patterns

### Pattern 1: Centralized Collector with DaemonSet

**Use Case:** Default pattern, minimal network hops

```yaml
# Collector DaemonSet
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector-daemonset
  namespace: observability
spec:
  mode: daemonset
  hostNetwork: true
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318
    
    processors:
      batch:
        timeout: 10s
        send_batch_size: 1024
      
      memory_limiter:
        check_interval: 1s
        limit_mib: 512
      
      resource:
        attributes:
          - key: k8s.cluster.name
            value: production-cluster
            action: upsert
    
    exporters:
      otlp:
        endpoint: backend-collector.observability.svc.cluster.local:4317
        tls:
          insecure: false
          cert_file: /certs/tls.crt
          key_file: /certs/tls.key
      
      logging:
        loglevel: debug
    
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [memory_limiter, batch, resource]
          exporters: [otlp, logging]
        metrics:
          receivers: [otlp]
          processors: [memory_limiter, batch, resource]
          exporters: [otlp]
        logs:
          receivers: [otlp]
          processors: [memory_limiter, batch, resource]
          exporters: [otlp]

---
# Instrumentation resource pointing to node-local collector
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: default-instrumentation
  namespace: default
spec:
  exporter:
    endpoint: http://${NODE_IP}:4317
  python:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-python:latest
```

### Pattern 2: Gateway Collector Pattern

**Use Case:** Additional processing, sampling, or routing

```yaml
# Gateway Collector (Deployment)
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector-gateway
  namespace: observability
spec:
  mode: deployment
  replicas: 3
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
    
    processors:
      batch:
        timeout: 10s
        send_batch_size: 2048
      
      tail_sampling:
        decision_wait: 10s
        num_traces: 100000
        expected_new_traces_per_sec: 1000
        policies:
          - name: errors-policy
            type: status_code
            status_code: {status_codes: [ERROR]}
          - name: slow-requests
            type: latency
            latency: {threshold_ms: 1000}
          - name: sample-percentage
            type: probabilistic
            probabilistic: {sampling_percentage: 10}
      
      k8sattributes:
        auth_type: "serviceAccount"
        passthrough: false
        extract:
          metadata:
            - k8s.namespace.name
            - k8s.deployment.name
            - k8s.statefulset.name
            - k8s.daemonset.name
            - k8s.cronjob.name
            - k8s.job.name
            - k8s.node.name
            - k8s.pod.name
            - k8s.pod.uid
            - k8s.pod.start_time
    
    exporters:
      otlp/jaeger:
        endpoint: jaeger-collector.observability.svc.cluster.local:4317
      
      prometheusremotewrite:
        endpoint: http://prometheus.observability.svc.cluster.local:9090/api/v1/write
      
      elasticsearch:
        endpoints: [https://elasticsearch.observability.svc.cluster.local:9200]
        logs_index: otel-logs
    
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [k8sattributes, tail_sampling, batch]
          exporters: [otlp/jaeger]
        metrics:
          receivers: [otlp]
          processors: [k8sattributes, batch]
          exporters: [prometheusremotewrite]
        logs:
          receivers: [otlp]
          processors: [k8sattributes, batch]
          exporters: [elasticsearch]

---
# Service for gateway collector
apiVersion: v1
kind: Service
metadata:
  name: otel-collector-gateway
  namespace: observability
spec:
  type: ClusterIP
  ports:
  - name: otlp-grpc
    port: 4317
    targetPort: 4317
  - name: otlp-http
    port: 4318
    targetPort: 4318
  selector:
    app.kubernetes.io/component: opentelemetry-collector
    app.kubernetes.io/instance: otel-collector-gateway
```

### Pattern 3: Sidecar Pattern

**Use Case:** Isolation, per-service configuration

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: sidecar
  namespace: default
spec:
  mode: sidecar
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: localhost:4317
    
    processors:
      batch:
    
    exporters:
      otlp:
        endpoint: otel-collector-gateway.observability.svc.cluster.local:4317
    
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch]
          exporters: [otlp]

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    metadata:
      annotations:
        instrumentation.opentelemetry.io/inject-python: "true"
        sidecar.opentelemetry.io/inject: "true"  # Inject collector sidecar
    spec:
      containers:
      - name: app
        image: my-app:latest
```

### Pattern 4: Multi-Cluster with Remote Write

**Use Case:** Multiple clusters sending to centralized observability

```yaml
# Each cluster has its own collector configuration
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: cluster-collector
spec:
  mode: deployment
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
    
    processors:
      batch:
      resource:
        attributes:
          - key: k8s.cluster.name
            value: ${CLUSTER_NAME}
            action: upsert
          - key: region
            value: ${REGION}
            action: upsert
    
    exporters:
      otlphttp:
        endpoint: https://central-collector.example.com:4318
        headers:
          Authorization: Bearer ${API_TOKEN}
    
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [resource, batch]
          exporters: [otlphttp]
```

---

## Security Considerations

### RBAC Configuration

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: opentelemetry-operator
  namespace: opentelemetry-operator-system

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: opentelemetry-operator
rules:
# Pods
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch", "update", "patch"]
# Instrumentation CRDs
- apiGroups: ["opentelemetry.io"]
  resources: ["instrumentations", "opentelemetrycollectors"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
# Webhook configuration
- apiGroups: ["admissionregistration.k8s.io"]
  resources: ["mutatingwebhookconfigurations", "validatingwebhookconfigurations"]
  verbs: ["get", "list", "watch", "create", "update", "patch"]
# Events
- apiGroups: [""]
  resources: ["events"]
  verbs: ["create", "patch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: opentelemetry-operator
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: opentelemetry-operator
subjects:
- kind: ServiceAccount
  name: opentelemetry-operator
  namespace: opentelemetry-operator-system
```

### Network Policies

```yaml
# Restrict collector network access
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: otel-collector-policy
  namespace: observability
spec:
  podSelector:
    matchLabels:
      app: opentelemetry-collector
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Allow OTLP from all namespaces
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 4317
    - protocol: TCP
      port: 4318
  egress:
  # Allow DNS
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: UDP
      port: 53
  # Allow to backend
  - to:
    - podSelector:
        matchLabels:
          app: jaeger
    ports:
    - protocol: TCP
      port: 4317
```

### Secret Management

```yaml
# Store sensitive exporter credentials
apiVersion: v1
kind: Secret
metadata:
  name: otel-collector-secrets
  namespace: observability
type: Opaque
stringData:
  backend-api-key: "your-api-key-here"
  backend-endpoint: "https://backend.example.com:4317"

---
# Reference secrets in collector
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector
spec:
  config: |
    exporters:
      otlphttp:
        endpoint: ${BACKEND_ENDPOINT}
        headers:
          api-key: ${BACKEND_API_KEY}
  env:
  - name: BACKEND_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: otel-collector-secrets
        key: backend-endpoint
  - name: BACKEND_API_KEY
    valueFrom:
      secretKeyRef:
        name: otel-collector-secrets
        key: backend-api-key
```

### TLS Configuration

```yaml
# Collector with TLS
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector-tls
spec:
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
            tls:
              cert_file: /certs/tls.crt
              key_file: /certs/tls.key
              client_ca_file: /certs/ca.crt
  volumeMounts:
  - name: certs
    mountPath: /certs
    readOnly: true
  volumes:
  - name: certs
    secret:
      secretName: otel-collector-tls
```

### Pod Security Standards

```yaml
# Enforce restricted pod security
apiVersion: v1
kind: Namespace
metadata:
  name: observability
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted

---
# Collector with security context
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: otel-collector-secure
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
    seccompProfile:
      type: RuntimeDefault
  podSecurityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
```

---

## Performance Impact

### Resource Overhead

**Memory Impact:**
```
Language    | Init Container | Runtime Overhead | Total Per Pod
------------|----------------|------------------|---------------
Java        | 100-200 MB     | 50-100 MB        | 150-300 MB
Python      | 50-100 MB      | 30-50 MB         | 80-150 MB
Node.js     | 50-100 MB      | 20-40 MB         | 70-140 MB
.NET        | 80-150 MB      | 40-80 MB         | 120-230 MB
Go (eBPF)   | 30-50 MB       | 10-20 MB         | 40-70 MB
```

**CPU Impact:**
```
Phase               | CPU Usage          | Duration
--------------------|--------------------|--------------
Init Container      | 100-500m           | 1-5 seconds
Application Startup | +10-30%            | 2-10 seconds
Steady State        | +5-15%             | Continuous
Sampling (10%)      | +2-5%              | Continuous
```

### Optimization Strategies

**1. Sampling Configuration:**
```yaml
spec:
  sampler:
    # Use parent-based sampling with low ratio
    type: parentbased_traceidratio
    argument: "0.01"  # 1% sampling for high-volume services
```

**2. Batch Processing:**
```yaml
processors:
  batch:
    timeout: 10s
    send_batch_size: 2048
    send_batch_max_size: 4096
```

**3. Memory Limiting:**
```yaml
processors:
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128
```

**4. Selective Instrumentation:**
```yaml
env:
# Disable instrumentations for internal/health endpoints
- name: OTEL_PYTHON_DISABLED_INSTRUMENTATIONS
  value: "urllib,urllib3"
# Or for Java
- name: OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED
  value: "false"
- name: OTEL_INSTRUMENTATION_HTTP_ENABLED
  value: "true"
- name: OTEL_INSTRUMENTATION_JDBC_ENABLED
  value: "true"
```

**5. Resource Requests/Limits:**
```yaml
python:
  resources:
    requests:
      cpu: 50m
      memory: 64Mi
    limits:
      cpu: 200m
      memory: 128Mi
```

### Monitoring Auto-Instrumentation

**Metrics to Track:**
```yaml
# Collector self-monitoring
processors:
  # Add prometheus exporter for collector metrics
  prometheus:
    endpoint: 0.0.0.0:8888

# Key metrics:
# - otelcol_receiver_accepted_spans
# - otelcol_receiver_refused_spans
# - otelcol_processor_batch_batch_send_size
# - otelcol_exporter_sent_spans
# - otelcol_exporter_send_failed_spans
```

---

## Best Practices

### 1. Gradual Rollout

**Phase 1: Canary Deployment**
```yaml
# Start with one namespace
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: canary-instrumentation
  namespace: canary
spec:
  sampler:
    type: always_on  # Sample everything initially
  python:
    image: your-image:latest
```

**Phase 2: Staged Rollout**
```bash
# Week 1: Dev environment (100% sampling)
# Week 2: Staging environment (50% sampling)
# Week 3: Production non-critical services (10% sampling)
# Week 4: Production critical services (1-5% sampling)
```

### 2. Service Naming Convention

```yaml
spec:
  resource:
    resourceAttributes:
      # Use consistent naming: <namespace>.<service>
      - key: service.name
        value: ${K8S_NAMESPACE}.${K8S_POD_LABELS_APP}
        action: upsert
      - key: service.namespace
        value: ${K8S_NAMESPACE}
        action: upsert
```

### 3. Environment-Specific Configuration

```yaml
# Development
---
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: dev-instrumentation
  namespace: dev
spec:
  sampler:
    type: always_on
  python:
    env:
    - name: OTEL_LOG_LEVEL
      value: debug

---
# Production
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: prod-instrumentation
  namespace: prod
spec:
  sampler:
    type: parentbased_traceidratio
    argument: "0.01"
  python:
    env:
    - name: OTEL_LOG_LEVEL
      value: info
```

### 4. Health Check Filtering

```yaml
# In custom span processor or collector
processors:
  filter:
    traces:
      span:
        - 'attributes["http.target"] == "/health"'
        - 'attributes["http.target"] == "/ready"'
        - 'attributes["http.target"] == "/metrics"'
```

### 5. Cost Optimization

**Sampling Strategy:**
```yaml
processors:
  tail_sampling:
    policies:
      # Always sample errors
      - name: error-policy
        type: status_code
        status_code: {status_codes: [ERROR]}
      
      # Always sample slow requests
      - name: slow-policy
        type: latency
        latency: {threshold_ms: 500}
      
      # Sample 1% of normal traffic
      - name: probabilistic-policy
        type: probabilistic
        probabilistic: {sampling_percentage: 1}
```

### 6. Version Control for Instrumentation

```yaml
# Tag instrumentation versions
metadata:
  name: instrumentation-v2
  labels:
    version: "2.0"
    instrumentation.opentelemetry.io/version: "0.42b0"
```

### 7. Testing Strategy

```yaml
# Test namespace with verbose logging
apiVersion: v1
kind: Namespace
metadata:
  name: otel-test

---
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: test-instrumentation
  namespace: otel-test
spec:
  sampler:
    type: always_on
  python:
    env:
    - name: OTEL_LOG_LEVEL
      value: debug
    - name: OTEL_TRACES_EXPORTER
      value: console,otlp
```

### 8. Documentation and Ownership

```yaml
metadata:
  annotations:
    owner: platform-team@company.com
    documentation: https://wiki.company.com/otel-instrumentation
    slack-channel: "#observability"
    runbook: https://runbooks.company.com/otel
```

### 9. Alerting on Instrumentation

```yaml
# Prometheus alert rules
groups:
- name: opentelemetry-instrumentation
  rules:
  - alert: OtelCollectorDown
    expr: up{job="opentelemetry-collector"} == 0
    for: 5m
    annotations:
      summary: "OpenTelemetry Collector is down"
  
  - alert: OtelHighDropRate
    expr: |
      rate(otelcol_receiver_refused_spans[5m]) /
      rate(otelcol_receiver_accepted_spans[5m]) > 0.1
    for: 10m
    annotations:
      summary: "High span drop rate detected"
  
  - alert: OtelInstrumentationFailing
    expr: |
      kube_pod_init_container_status_ready{
        container="opentelemetry-auto-instrumentation"
      } == 0
    for: 5m
    annotations:
      summary: "Auto-instrumentation init container failing"
```

### 10. Migration Path

**From Manual to Auto-Instrumentation:**
```yaml
# Step 1: Deploy alongside manual instrumentation
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-python: "true"
env:
# Keep manual instrumentation but disabled
- name: OTEL_SDK_DISABLED
  value: "true"  # Disable manual SDK

# Step 2: Validate auto-instrumentation works

# Step 3: Remove manual instrumentation code

# Step 4: Remove OTEL_SDK_DISABLED flag
```

---

## Troubleshooting Guide

### Common Issues

**1. Init Container Fails**
```bash
# Check init container logs
kubectl logs <pod-name> -c opentelemetry-auto-instrumentation-python

# Common causes:
# - Image pull failures
# - Insufficient permissions
# - Volume mount issues
```

**2. No Telemetry Data**
```bash
# Verify environment variables
kubectl exec <pod-name> -- env | grep OTEL

# Check SDK is loaded
kubectl exec <pod-name> -- python -c "import opentelemetry; print(opentelemetry.__version__)"

# Test collector connectivity
kubectl exec <pod-name> -- curl -v http://otel-collector:4317
```

**3. High Memory Usage**
```yaml
# Reduce batch size
processors:
  batch:
    send_batch_size: 512  # Reduce from default 8192
    timeout: 5s
```

**4. Instrumentation Not Applied**
```bash
# Check webhook is registered
kubectl get mutatingwebhookconfigurations

# Check operator logs
kubectl logs -n opentelemetry-operator-system deployment/opentelemetry-operator

# Verify annotation syntax
kubectl get pod <pod-name> -o yaml | grep instrumentation
```

---

## Appendix

### A. Complete Example Application

```yaml
# Complete setup for a Python Flask application
---
apiVersion: v1
kind: Namespace
metadata:
  name: my-app

---
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: my-app-instrumentation
  namespace: my-app
spec:
  exporter:
    endpoint: http://otel-collector.observability.svc.cluster.local:4317
  propagators:
    - tracecontext
    - baggage
  sampler:
    type: parentbased_traceidratio
    argument: "0.1"
  python:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-python:latest
    env:
    - name: OTEL_PYTHON_LOGGING_AUTO_INSTRUMENTATION_ENABLED
      value: "true"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flask-app
  namespace: my-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: flask-app
  template:
    metadata:
      annotations:
        instrumentation.opentelemetry.io/inject-python: "true"
      labels:
        app: flask-app
    spec:
      containers:
      - name: app
        image: my-flask-app:latest
        ports:
        - containerPort: 5000
        env:
        - name: FLASK_APP
          value: app.py
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi

---
apiVersion: v1
kind: Service
metadata:
  name: flask-app
  namespace: my-app
spec:
  selector:
    app: flask-app
  ports:
  - port: 80
    targetPort: 5000
```

### B. Useful Commands

```bash
# Install operator
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml

# Check operator status
kubectl get pods -n opentelemetry-operator-system

# List instrumentations
kubectl get instrumentations --all-namespaces

# Describe instrumentation
kubectl describe instrumentation <name> -n <namespace>

# Check if pod is instrumented
kubectl get pod <pod-name> -o yaml | grep -A 10 "annotations:"

# View injected environment variables
kubectl exec <pod-name> -- env | grep OTEL

# Test telemetry flow
kubectl port-forward svc/otel-collector 4317:4317
# Then send test span using otel CLI or curl

# Debug collector
kubectl logs -n observability deployment/otel-collector -f

# Check webhook
kubectl get mutatingwebhookconfigurations opentelemetry-operator-mutation -o yaml
```

### C. References

- [OpenTelemetry Specification](https://opentelemetry.io/docs/specs/otel/)
- [OpenTelemetry Operator GitHub](https://github.com/open-telemetry/opentelemetry-operator)
- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)
- [Auto-Instrumentation Language Support](https://opentelemetry.io/docs/instrumentation/)
- [OTLP Protocol Specification](https://opentelemetry.io/docs/specs/otlp/)

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-16  
**Maintained By:** Platform Engineering Team