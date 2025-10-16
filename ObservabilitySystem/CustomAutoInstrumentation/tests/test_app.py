#!/usr/bin/env python3
# Simple Flask application to test custom auto-instrumentation

from flask import Flask, jsonify
import time
import os
import logging

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

if __name__ == '__main__':
    # Get port from environment or default to 8080
    port = int(os.environ.get('PORT', 8080))
    
    logger.info(f"Starting test application on port {port}")
    logger.info("Available endpoints: /, /slow, /error")
    
    # Run the Flask app
    app.run(host='0.0.0.0', port=port)