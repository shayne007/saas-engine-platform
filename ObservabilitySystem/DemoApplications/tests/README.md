# Test Application
## This can be run as a normal Flask application.

```bash
docker build -t test-app:latest -f ./Dockerfile .

# run docker container using docker-compose.yaml, you may need to check the image version of test-app.
docker-compose up -d
docker-compose down && docker-compose up -d
```

## This can be also run with auto-instrumentation in k8s

after preparation for the operator and CRD in the cluster,
we can auto inject the python lib with one single configuratiion
```yaml
# other code...
    metadata:
      labels:
        app: test-app
      annotations:
        instrumentation.opentelemetry.io/inject-python: "true"
# other code...
```

```bash
# in k8s cluster apply the deployment
kubectl apply -f k8s-deployment-auto-instumentation.yaml
```