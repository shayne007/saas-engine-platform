# Custom Auto-Instrumentation Helm Chart

This chart deploys:
- OpenTelemetry `Instrumentation` CR for Python auto-instrumentation
- Optional OpenTelemetry Collector (Deployment + Service)
- Optional test application to validate instrumentation
- Optional OpenTelemetry Operator (for convenience; recommend using official chart in prod)

## Prerequisites
- Kubernetes cluster and `kubectl`
- Helm v3
- OpenTelemetry Operator installed (recommended):
  - Official chart: https://github.com/open-telemetry/opentelemetry-helm-charts/tree/main/charts/opentelemetry-operator

## Install

### Development
```bash
helm install custom-auto-instr ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation \
  -n observability --create-namespace \
  -f ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation/values-dev.yaml
```

### Test
```bash
helm upgrade --install custom-auto-instr ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation \
  -n observability --create-namespace \
  -f ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation/values-test.yaml
```

### Production
```bash
helm upgrade --install custom-auto-instr ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation \
  -n observability --create-namespace \
  -f ./ObservabilitySystem/CustomAutoInstrumentation/helm/custom-auto-instrumentation/values-prod.yaml
```

## Key Values
- `environment`: sets resource attribute `deployment.environment`
- `instrumentation.*`: defines the Instrumentation CR (exporter endpoint, propagators, sampler, python image and env)
- `collector.enabled`: toggle Collector deployment and config map
- `collector.exporters.*`: configure logging, OTLP, and Prometheus exporters
- `testApp.enabled`: deploy a simple test app with python injection
- `operator.enabled`: deploy operator (not recommended for prod; use official chart)

## Validate
```bash
kubectl get instrumentation.opentelemetry.io -A
kubectl get deploy,svc -n observability
```

## Uninstall
```bash
helm uninstall custom-auto-instr -n observability
```