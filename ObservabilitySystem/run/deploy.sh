#!/bin/bash

# Observability System Deployment Script
# This script deploys the complete observability system to Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="observability"
CONTEXT=""

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    print_success "kubectl is installed"
    
    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        print_warning "helm is not installed. Some features may not work."
    else
        print_success "helm is installed"
    fi
    
    # Check kubectl context
    if [ -n "$CONTEXT" ]; then
        kubectl config use-context "$CONTEXT"
        print_success "Switched to context: $CONTEXT"
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster. Please check your configuration."
        exit 1
    fi
    print_success "Connected to Kubernetes cluster"
}

create_namespace() {
    print_header "Creating Namespace"
    
    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        print_warning "Namespace $NAMESPACE already exists"
    else
        kubectl create namespace "$NAMESPACE"
        print_success "Created namespace: $NAMESPACE"
    fi
}

install_otel_operator() {
    print_header "Installing OpenTelemetry Operator"
    
    # Add OpenTelemetry Helm repository
    helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
    helm repo update
    
    # Install OpenTelemetry Operator
    helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
        --namespace "$NAMESPACE" \
        --create-namespace \
        --wait
    
    print_success "OpenTelemetry Operator installed"
}

deploy_otel_collector() {
    print_header "Deploying OpenTelemetry Collector"
    
    # Deploy DaemonSet collector
    kubectl apply -f KubernetesOperator/otel-collector-daemonset.yaml
    
    # Deploy Gateway collector
    kubectl apply -f KubernetesOperator/otel-collector-gateway.yaml
    
    # Wait for collectors to be ready
    kubectl wait --for=condition=available --timeout=300s deployment/otel-collector-gateway -n "$NAMESPACE"
    kubectl wait --for=condition=ready --timeout=300s daemonset/otel-collector -n "$NAMESPACE"
    
    print_success "OpenTelemetry Collector deployed"
}

deploy_storage_backends() {
    print_header "Deploying Storage Backends"
    
    # Deploy Prometheus
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo update
    
    helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
        --namespace "$NAMESPACE" \
        --set prometheus.prometheusSpec.retention=7d \
        --set grafana.adminPassword=admin123 \
        --wait
    
    # Deploy Tempo
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo update
    
    helm upgrade --install tempo grafana/tempo \
        --namespace "$NAMESPACE" \
        --wait
    
    # Deploy Loki
    helm upgrade --install loki grafana/loki-stack \
        --namespace "$NAMESPACE" \
        --wait
    
    print_success "Storage backends deployed"
}

deploy_grafana() {
    print_header "Deploying Grafana"
    
    # Deploy Grafana with custom configuration
    kubectl apply -f GrafanaDashboards/grafana-deployment.yaml
    
    # Wait for Grafana to be ready
    kubectl wait --for=condition=available --timeout=300s deployment/grafana -n "$NAMESPACE"
    
    print_success "Grafana deployed"
}

deploy_demo_applications() {
    print_header "Deploying Demo Applications"
    
    # Deploy auto-instrumentation configuration
    kubectl apply -f KubernetesOperator/auto-instrumentation.yaml
    
    print_success "Demo applications deployed"
}

create_docker_images() {
    print_header "Building Docker Images"
    
    # Build Data Collector Service
    if [ -f "DataCollectorService/Dockerfile" ]; then
        docker build -t observability/data-collector:latest DataCollectorService/
        print_success "Data Collector Service image built"
    fi
    
    # Build Storage Aggregation Service
    if [ -f "StorageAggregationService/Dockerfile" ]; then
        docker build -t observability/storage-aggregation:latest StorageAggregationService/
        print_success "Storage Aggregation Service image built"
    fi
    
    # Build Python demo applications
    if [ -f "DemoApplications/PythonDemos/Dockerfile.page-view-tracker" ]; then
        docker build -f DemoApplications/PythonDemos/Dockerfile.page-view-tracker -t observability/page-view-tracker:latest DemoApplications/PythonDemos/
        print_success "Page View Tracker image built"
    fi
    
    if [ -f "DemoApplications/PythonDemos/Dockerfile.api-monitor" ]; then
        docker build -f DemoApplications/PythonDemos/Dockerfile.api-monitor -t observability/api-monitor:latest DemoApplications/PythonDemos/
        print_success "API Monitor image built"
    fi
}

get_service_urls() {
    print_header "Service URLs"
    
    # Get Grafana URL
    GRAFANA_URL=$(kubectl get service grafana -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    if [ -z "$GRAFANA_URL" ]; then
        GRAFANA_URL="localhost:3000"
        print_warning "Grafana LoadBalancer not ready. Use port-forward:"
        echo "kubectl port-forward -n $NAMESPACE svc/grafana 3000:80"
    fi
    
    echo -e "${GREEN}📊 Grafana Dashboard:${NC} http://$GRAFANA_URL"
    echo -e "${GREEN}🔍 Prometheus:${NC} http://localhost:9090 (port-forward required)"
    echo -e "${GREEN}📈 Tempo:${NC} http://localhost:3200 (port-forward required)"
    echo -e "${GREEN}📋 Loki:${NC} http://localhost:3100 (port-forward required)"
    
    echo ""
    echo -e "${YELLOW}Default Grafana credentials:${NC}"
    echo "Username: admin"
    echo "Password: admin123"
}

run_health_checks() {
    print_header "Running Health Checks"
    
    # Check if all pods are running
    NOT_READY=$(kubectl get pods -n "$NAMESPACE" --field-selector=status.phase!=Running --no-headers | wc -l)
    
    if [ "$NOT_READY" -eq 0 ]; then
        print_success "All pods are running"
    else
        print_warning "$NOT_READY pods are not ready yet"
        kubectl get pods -n "$NAMESPACE"
    fi
    
    # Check services
    kubectl get services -n "$NAMESPACE"
}

cleanup() {
    print_header "Cleaning Up"
    
    read -p "Are you sure you want to delete the observability system? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kubectl delete namespace "$NAMESPACE"
        print_success "Observability system cleaned up"
    else
        print_warning "Cleanup cancelled"
    fi
}

show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  deploy     Deploy the complete observability system"
    echo "  build      Build Docker images only"
    echo "  status     Show deployment status"
    echo "  cleanup    Remove the observability system"
    echo "  help       Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  NAMESPACE  Kubernetes namespace (default: observability)"
    echo "  CONTEXT    Kubernetes context to use"
    echo ""
    echo "Examples:"
    echo "  $0 deploy"
    echo "  NAMESPACE=my-observability $0 deploy"
    echo "  CONTEXT=my-cluster $0 deploy"
}

main() {
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            create_namespace
            install_otel_operator
            deploy_otel_collector
            deploy_storage_backends
            deploy_grafana
            deploy_demo_applications
            run_health_checks
            get_service_urls
            print_success "Observability system deployed successfully!"
            ;;
        "build")
            check_prerequisites
            create_docker_images
            ;;
        "status")
            print_header "Deployment Status"
            kubectl get pods -n "$NAMESPACE"
            kubectl get services -n "$NAMESPACE"
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"-h"|"--help")
            show_usage
            ;;
        *)
            print_error "Unknown command: $1"
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
