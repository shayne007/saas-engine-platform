#!/usr/bin/env python3
# Simple Flask application with direct OpenTelemetry instrumentation

from flask import Flask, jsonify, request
import time
import os
import logging
import sys

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Import OpenTelemetry components
try:
    from opentelemetry import trace
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
    from opentelemetry.sdk.resources import Resource
    # Use HTTP exporter instead of gRPC
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
    from opentelemetry.instrumentation.flask import FlaskInstrumentor
    
    # Import custom span processor if available
    sys.path.append('/auto-instrumentation')
    try:
        from extensions.custom_span_processor import CustomSpanProcessor
        logger.info("Successfully imported CustomSpanProcessor")
    except ImportError:
        logger.warning("Could not import CustomSpanProcessor, using standard processor")
        CustomSpanProcessor = None
except ImportError as e:
    logger.error(f"Error importing OpenTelemetry: {e}")
    logger.error("Running without telemetry")
    trace = None

# Create Flask app
app = Flask(__name__)

# Initialize OpenTelemetry if available
if trace:
    # Configure OpenTelemetry
    resource = Resource.create({
        "service.name": os.environ.get("OTEL_SERVICE_NAME", "test-app"),
        "deployment.environment": "local-testing"
    })
    
    # Set up tracer provider
    tracer_provider = TracerProvider(resource=resource)
    trace.set_tracer_provider(tracer_provider)
    
    # Set up OTLP exporter using HTTP
    # 使用HTTP导出器，指向otel-collector的HTTP端口4318，并包含正确的路径
    otlp_exporter = OTLPSpanExporter(
        endpoint=os.environ.get("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://otel-collector:4318/v1/traces")
    )
    
    # Add standard span processor
    tracer_provider.add_span_processor(BatchSpanProcessor(otlp_exporter))
    
    # Add custom span processor if available
    if CustomSpanProcessor:
        tracer_provider.add_span_processor(CustomSpanProcessor())
        logger.info("Added CustomSpanProcessor to tracer provider")
    
    # Instrument Flask
    FlaskInstrumentor().instrument_app(app)
    
    # Get tracer
    tracer = trace.get_tracer(__name__)
    
    logger.info("OpenTelemetry initialized successfully")
else:
    logger.warning("OpenTelemetry not available, running without instrumentation")
    tracer = None

@app.route('/')
def home():
    logger.info("Home endpoint called")
    return jsonify({"status": "ok", "message": "Test application is running"})

@app.route('/slow')
def slow_endpoint():
    logger.info("Slow endpoint called")
    # Simulate slow operation that will trigger custom span processor
    time.sleep(0.2)  # 200ms, above the 100ms threshold in custom_span_processor
    return jsonify({"status": "ok", "message": "Slow operation completed"})

@app.route('/error')
def error_endpoint():
    logger.info("Error endpoint called")
    # Simulate an error that will trigger custom span processor
    try:
        raise ValueError("Test error")
    except ValueError as e:
        logger.error(f"Error occurred: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/manual-span')
def manual_span():
    if not tracer:
        return jsonify({"status": "error", "message": "OpenTelemetry not available"}), 500
        
    logger.info("Manual span endpoint called")
    
    # Create a custom span
    with tracer.start_as_current_span("manual.operation") as span:
        # Add custom attributes
        span.set_attribute("custom.attribute", "test-value")
        span.set_attribute("http.request.id", request.headers.get("X-Request-ID", "unknown"))
        
        # Simulate some work
        time.sleep(0.1)
        
    return jsonify({"status": "ok", "message": "Manual span created"})

if __name__ == '__main__':
    # Get port from environment or default to 8080
    port = int(os.environ.get('PORT', 8080))
    
    logger.info(f"Starting test application on port {port}")
    logger.info("Available endpoints: /, /slow, /error, /manual-span")
    
    # Run the Flask app
    app.run(host='0.0.0.0', port=port)