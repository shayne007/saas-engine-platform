#!/bin/bash
# Script to build auto-instrumentation images for multiple Python versions

set -e

# Default versions to build
# PYTHON_VERSIONS=("3.8" "3.9" "3.10" "3.11" "3.12")
PYTHON_VERSIONS=("3.10" "3.12")
IMAGE_NAME="custom-otel-autoinstrumentation-python"
IMAGE_TAG_PREFIX="1.0.0-python"
RETRY_COUNT=3
RETRY_DELAY=5
OFFLINE_MODE=false
DOCKER_REGISTRY="docker.io/library"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --versions)
      IFS=',' read -ra PYTHON_VERSIONS <<< "$2"
      shift 2
      ;;
    --image-name)
      IMAGE_NAME="$2"
      shift 2
      ;;
    --tag-prefix)
      IMAGE_TAG_PREFIX="$2"
      shift 2
      ;;
    --offline)
      OFFLINE_MODE=true
      shift
      ;;
    --registry)
      DOCKER_REGISTRY="$2"
      shift 2
      ;;
    --retries)
      RETRY_COUNT="$2"
      shift 2
      ;;
    --retry-delay)
      RETRY_DELAY="$2"
      shift 2
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Build auto-instrumentation images for multiple Python versions"
      echo ""
      echo "Options:"
      echo "  --versions      Comma-separated list of Python versions (default: 3.8,3.9,3.10,3.11,3.12)"
      echo "  --image-name    Base image name (default: custom-otel-autoinstrumentation-python)"
      echo "  --tag-prefix    Image tag prefix (default: 1.0.0-python)"
      echo "  --offline       Build in offline mode, using locally available base images only"
      echo "  --registry      Specify alternative Docker registry (default: docker.io/library)"
      echo "  --retries       Number of retries for Docker operations (default: 3)"
      echo "  --retry-delay   Delay between retries in seconds (default: 5)"
      echo "  --help          Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "Building auto-instrumentation images for Python versions: ${PYTHON_VERSIONS[*]}"

# Check for version-specific requirements files
for version in "${PYTHON_VERSIONS[@]}"; do
  if [ ! -f "requirements-$version.txt" ]; then
    echo "Warning: No version-specific requirements file found for Python $version (requirements-$version.txt)"
    echo "Will use default requirements.txt for this version"
  else
    echo "Found version-specific requirements for Python $version"
  fi
done

# Function to execute Docker commands with retry
docker_cmd_with_retry() {
  local cmd="$1"
  local attempt=1
  local max_attempts=$RETRY_COUNT
  
  while [ $attempt -le $max_attempts ]; do
    echo "Attempt $attempt of $max_attempts: $cmd"
    if eval "$cmd"; then
      return 0
    else
      echo "Command failed. Retrying in $RETRY_DELAY seconds..."
      sleep $RETRY_DELAY
      attempt=$((attempt + 1))
    fi
  done
  
  echo "Error: Command failed after $max_attempts attempts"
  return 1
}

# Build images for each Python version
for version in "${PYTHON_VERSIONS[@]}"; do
  echo "Building image for Python $version..."
  
  # Prepare build arguments
  build_args=(
    "--build-arg" "PYTHON_VERSION=$version"
  )
  
  # Prepare environment prefix for docker commands (timeout and proxy)
  env_prefix=""
  if [ -n "$HTTP_PROXY" ]; then
    env_prefix="HTTP_PROXY=$HTTP_PROXY $env_prefix"
  fi
  if [ -n "$HTTPS_PROXY" ]; then
    env_prefix="HTTPS_PROXY=$HTTPS_PROXY $env_prefix"
  fi
  if [ -n "$DOCKER_TIMEOUT" ]; then
    env_prefix="DOCKER_CLIENT_TIMEOUT=$DOCKER_TIMEOUT $env_prefix"
  fi
  
  # Add registry override if not in offline mode
  if [ "$OFFLINE_MODE" = false ]; then
    build_args+=("--build-arg" "BASE_IMAGE_REGISTRY=$DOCKER_REGISTRY")
  else
    echo "Building in offline mode - using locally available images only"
    # Add network=none if in offline mode
    build_args+=("--network=none")
  fi
  
  # Build the image with the specific Python version
  build_cmd="$env_prefix docker build ${build_args[*]} -t \"$IMAGE_NAME:$IMAGE_TAG_PREFIX$version\" ."
  
  if ! docker_cmd_with_retry "$build_cmd"; then
    echo "Warning: Failed to build image for Python $version"
    echo "Try using --offline mode if you have connectivity issues"
    echo "Or specify an alternative registry with --registry"
    continue
  fi
  
  echo "Successfully built $IMAGE_NAME:$IMAGE_TAG_PREFIX$version"
done

# Create a manifest for the latest tag if we have successfully built images
echo "Creating manifest for latest tag..."
manifest_cmd="$env_prefix docker manifest create \"$IMAGE_NAME:latest\" $(printf "$IMAGE_NAME:$IMAGE_TAG_PREFIX%s " "${PYTHON_VERSIONS[@]}")"
docker_cmd_with_retry "$manifest_cmd" || echo "Warning: Failed to create manifest for latest tag"

echo "All images built successfully!"
echo ""
echo "To use a specific Python version in your deployment, add the following annotation to your pod:"
echo "instrumentation.opentelemetry.io/python-version: \"X.Y\""
echo ""
echo "Available images:"
for version in "${PYTHON_VERSIONS[@]}"; do
  echo "- $IMAGE_NAME:$IMAGE_TAG_PREFIX$version (with version-specific dependencies)"
done
echo "- $IMAGE_NAME:latest (manifest pointing to all versions)"