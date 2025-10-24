# Introduction
## Build the test app docker image
```bash
cd ObservabilitySystem/DemoApplications/TestManualInstrumention
docker build -t test-app:latest -f ./Dockerfile .
docker image tag  test-app:latest test-app:1.0.0-man-inst 

```
## 🧪 Testing

The project includes a test application for validating the instrumentation:

```bash
# Start the test environment
docker-compose up -d
docker-compose down && docker-compose up -d

# Test manual span creation
curl http://localhost:8080/manual-span

# Test slow operation span
curl http://localhost:8080/slow

# Check logs for span processing
docker logs customautoinstrumentation-test-app-1 | grep -E 'custom_span_processor'
```