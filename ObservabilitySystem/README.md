# Observability System

A comprehensive observability system built with OpenTelemetry that supports collecting all kinds of telemetry data including user login, page viewing, API calling, error logs, and business user behavior tracking and monitoring.

## 🏗️ Architecture Overview

The system consists of several key components:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Demo Apps     │───▶│ Data Collector   │───▶│ Storage &       │
│ (Java/Python)   │    │    Service       │    │ Aggregation     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │ OTel Collector   │    │ Grafana         │
                       │ (DaemonSet)      │    │ Dashboards      │
                       └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ Storage Backends │
                       │ (Prometheus,     │
                       │  Tempo, Loki)    │
                       └──────────────────┘
```

## 🚀 Core Features

### ✅ Data Collection
- **Multi-language Support**: Java and Python SDKs with auto-instrumentation
- **Comprehensive Telemetry**: Metrics, traces, and logs collection
- **User Behavior Tracking**: Login events, page views, API calls
- **Error Monitoring**: Detailed error tracking with context
- **Business Metrics**: Custom KPIs and conversion tracking

### ✅ Storage & Processing
- **OpenTelemetry Stack**: Native OTel collectors and exporters
- **Time-series Storage**: Prometheus for metrics
- **Distributed Tracing**: Tempo for trace storage
- **Log Aggregation**: Loki for log management
- **Data Aggregation**: Real-time processing and metrics generation

### ✅ Visualization & Monitoring
- **Grafana Dashboards**: Comprehensive monitoring views
- **Real-time Metrics**: Live system performance monitoring
- **API Call Tracing**: End-to-end request flow visualization
- **Business Analytics**: User behavior and conversion analysis
- **Alerting**: Configurable alerts for critical metrics

### ✅ Kubernetes Integration
- **Auto-instrumentation**: Zero-code instrumentation with operators
- **Scalable Deployment**: Kubernetes-native architecture
- **Resource Management**: Optimized resource allocation
- **High Availability**: Multi-replica deployments

## 📦 Components

### 1. DataCollectorService
- **Purpose**: Central telemetry data collection endpoint
- **Technology**: Spring Boot with OpenTelemetry Java SDK
- **Port**: 8081
- **Features**:
  - REST API for telemetry data ingestion
  - Real-time metrics generation
  - Distributed tracing support
  - Multi-format data support (JSON, Protobuf)

### 2. StorageAggregationService
- **Purpose**: Data storage and aggregation processing
- **Technology**: Spring Boot with JPA and Redis
- **Port**: 8082
- **Features**:
  - Database persistence (PostgreSQL)
  - Real-time aggregation
  - Scheduled data processing
  - Data retention management

### 3. DemoApplications
- **UserLoginDemo**: Java Spring Boot application demonstrating login tracking
- **PageViewTracker**: Python Flask application for page view analytics
- **ApiMonitor**: Python application for API call monitoring
- **ErrorTracker**: Error logging and monitoring
- **BusinessMetricsDemo**: Business KPIs tracking

### 4. KubernetesOperator
- **OpenTelemetry Collector**: DaemonSet for node-level collection
- **Gateway Collector**: Cluster-level aggregation and routing
- **Auto-instrumentation**: Operator for automatic instrumentation
- **RBAC Configuration**: Proper security and permissions

### 5. GrafanaDashboards
- **Observability Dashboard**: Comprehensive system monitoring
- **Service Health**: Real-time service status
- **Business Metrics**: User behavior and conversion tracking
- **Error Analysis**: Error trends and patterns

## 🛠️ Quick Start

### Prerequisites
- Java 17+
- Python 3.8+
- Docker and Docker Compose
- Kubernetes cluster (minikube, GKE, EKS, etc.)
- kubectl configured

### 1. Clone and Build

```bash
git clone <repository-url>
cd ObservabilitySystem

# Build Java services
mvn clean package -DskipTests

# Install Python dependencies
pip install -r requirements.txt
```

### 2. Local Development Setup

```bash
# Start infrastructure services
docker-compose up -d prometheus tempo loki grafana

# Start data collector service
cd DataCollectorService
mvn spring-boot:run

# Start Python demo applications
cd DemoApplications/PythonDemos
python page_view_tracker.py &
python api_monitor.py &
```

### 3. Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace observability

# Deploy OpenTelemetry Collector
kubectl apply -f KubernetesOperator/otel-collector-daemonset.yaml
kubectl apply -f KubernetesOperator/otel-collector-gateway.yaml

# Deploy Grafana
kubectl apply -f GrafanaDashboards/grafana-deployment.yaml

# Deploy demo applications
kubectl apply -f KubernetesOperator/auto-instrumentation.yaml
```

### 4. Access Services

- **Grafana Dashboard**: http://localhost:3000 (admin/admin123)
- **Data Collector API**: http://localhost:8081/api/telemetry
- **Page View Tracker**: http://localhost:8082
- **API Monitor**: http://localhost:8083

## 📊 Usage Examples

### 1. Track User Login

```bash
curl -X POST http://localhost:8081/api/telemetry/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user123",
    "method": "password",
    "success": true,
    "duration_ms": 250,
    "ip_address": "192.168.1.100"
  }'
```

### 2. Track Page View

```bash
curl -X POST http://localhost:8082/api/pageview \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user123",
    "page_name": "product_detail",
    "page_url": "/products/123",
    "load_time_ms": 1200,
    "viewport_width": 1920,
    "viewport_height": 1080
  }'
```

### 3. Monitor API Call

```bash
curl -X POST http://localhost:8083/api/monitor/call \
  -H "Content-Type: application/json" \
  -d '{
    "endpoint": "/api/orders",
    "method": "POST",
    "status_code": 201,
    "duration_ms": 150,
    "user_id": "user123"
  }'
```

### 4. Record Error Event

```bash
curl -X POST http://localhost:8081/api/telemetry/error \
  -H "Content-Type: application/json" \
  -d '{
    "error_type": "validation_error",
    "message": "Invalid email format",
    "severity": "medium",
    "user_id": "user123",
    "service_name": "user-service"
  }'
```

## 📈 Grafana Dashboard Features

### Service Health Dashboard
- **Request Rate**: Real-time RPS by service
- **Error Rate**: Error percentage with thresholds
- **Latency Percentiles**: P50, P95, P99 response times
- **Service Dependencies**: Visual service map

### Business Metrics Dashboard
- **Login Success Rate**: Authentication success tracking
- **Page Views**: Popular pages and user navigation
- **Active Users**: Real-time user activity
- **Conversion Funnel**: User journey analysis

### Error Analysis Dashboard
- **Error Distribution**: Error types and frequencies
- **Slow Endpoints**: Performance bottleneck identification
- **Error Trends**: Historical error patterns
- **Service Impact**: Error impact on business metrics

## 🔧 Configuration

### Environment Variables

```bash
# OpenTelemetry Configuration
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=your-service-name
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/observability
REDIS_URL=redis://localhost:6379

# Service Configuration
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=production
```

### Kubernetes Annotations for Auto-instrumentation

```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-java: "true"
    instrumentation.opentelemetry.io/inject-python: "true"
```

## 📋 Monitoring & Alerting

### Key Metrics to Monitor

1. **System Health**
   - Request rate and error rate
   - Response time percentiles
   - Service availability

2. **Business Metrics**
   - User login success rate
   - Page view conversion
   - API usage patterns

3. **Error Tracking**
   - Error frequency and types
   - Service-specific errors
   - Error impact analysis

### Alerting Rules

```yaml
# High Error Rate Alert
- alert: HighErrorRate
  expr: api_error_rate > 0.05
  for: 5m
  annotations:
    summary: "High error rate detected"

# Login Success Rate Alert
- alert: LowLoginSuccessRate
  expr: login_success_rate < 0.90
  for: 15m
  annotations:
    summary: "Login success rate is low"
```

## 🚀 Production Deployment

### 1. Resource Requirements

```yaml
# Minimum cluster requirements
resources:
  requests:
    memory: 2Gi
    cpu: 1000m
  limits:
    memory: 4Gi
    cpu: 2000m
```

### 2. Scaling Configuration

```yaml
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: data-collector-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: data-collector-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 3. Data Retention

```yaml
# Prometheus retention
retention: 7d
retentionSize: 10GB

# Tempo retention
retention_period: 7d
```

## 🔍 Troubleshooting

### Common Issues

1. **OTel Collector Not Receiving Data**
   ```bash
   # Check collector logs
   kubectl logs -n observability daemonset/otel-collector
   
   # Verify endpoint connectivity
   kubectl port-forward -n observability svc/otel-collector 4317:4317
   ```

2. **Grafana Dashboard Not Loading**
   ```bash
   # Check datasource configuration
   kubectl get configmap -n observability grafana-datasources
   
   # Verify Prometheus connectivity
   kubectl port-forward -n observability svc/prometheus 9090:9090
   ```

3. **High Memory Usage**
   ```bash
   # Adjust batch sizes in collector config
   batch:
     send_batch_size: 512
     timeout: 10s
   ```

## 📚 Additional Resources

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana Dashboard Library](https://grafana.com/grafana/dashboards/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/)
- [Kubernetes Observability](https://kubernetes.io/docs/tasks/debug-application-cluster/resource-usage-monitoring/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the documentation links

---

**Built with ❤️ using OpenTelemetry, Spring Boot, Python, and Kubernetes**
