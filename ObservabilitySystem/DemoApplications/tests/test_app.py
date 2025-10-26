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

# Create Flask app
app = Flask(__name__)

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