#!/usr/bin/env python3
"""
API Monitor Demo
Demonstrates API call monitoring with OpenTelemetry instrumentation.
"""

import os
import time
import random
import logging
import asyncio
from datetime import datetime
from typing import Dict, Any, Optional

from flask import Flask, request, jsonify
from opentelemetry import trace, metrics
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.exporter.otlp.proto.grpc.metric_exporter import OTLPMetricExporter
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.sdk.resources import Resource
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
import requests

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)

# Configure OpenTelemetry
def setup_telemetry():
    """Setup OpenTelemetry tracing and metrics."""
    
    # Create resource
    resource = Resource.create({
        "service.name": "api-monitor",
        "service.version": "1.0.0",
        "deployment.environment": os.getenv("ENVIRONMENT", "development"),
    })
    
    # Setup tracing
    trace_provider = TracerProvider(resource=resource)
    trace_provider.add_span_processor(
        BatchSpanProcessor(
            OTLPSpanExporter(
                endpoint=os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"),
                insecure=True
            )
        )
    )
    trace.set_tracer_provider(trace_provider)
    
    # Setup metrics
    metric_reader = PeriodicExportingMetricReader(
        OTLPMetricExporter(
            endpoint=os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"),
            insecure=True
        )
    )
    meter_provider = MeterProvider(
        resource=resource,
        metric_readers=[metric_reader]
    )
    metrics.set_meter_provider(meter_provider)
    
    # Auto-instrument Flask and requests
    FlaskInstrumentor().instrument_app(app)
    RequestsInstrumentor().instrument()
    
    logger.info("OpenTelemetry setup completed")

# Initialize telemetry
setup_telemetry()

# Get tracer and meter
tracer = trace.get_tracer(__name__)
meter = metrics.get_meter(__name__)

# Create metrics
api_call_counter = meter.create_counter(
    name="api.calls.total",
    description="Total API calls",
    unit="1"
)

api_duration_histogram = meter.create_histogram(
    name="api.duration",
    description="API call duration in milliseconds",
    unit="ms"
)

api_error_counter = meter.create_counter(
    name="api.errors.total",
    description="Total API errors",
    unit="1"
)

# In-memory storage for demo
api_calls = []
api_endpoints = {}

class ApiCallEvent:
    """API call event model."""
    
    def __init__(self, endpoint: str, method: str, status_code: int, duration_ms: int, **kwargs):
        self.endpoint = endpoint
        self.method = method
        self.status_code = status_code
        self.duration_ms = duration_ms
        self.timestamp = datetime.now()
        self.user_id = kwargs.get('user_id')
        self.trace_id = kwargs.get('trace_id')
        self.span_id = kwargs.get('span_id')
        self.error_message = kwargs.get('error_message')
        self.request_size_bytes = kwargs.get('request_size_bytes', random.randint(100, 10000))
        self.response_size_bytes = kwargs.get('response_size_bytes', random.randint(50, 5000))
        self.ip_address = kwargs.get('ip_address')
        self.user_agent = kwargs.get('user_agent')

@app.route('/api/monitor/call', methods=['POST'])
def monitor_api_call():
    """Monitor an API call event."""
    
    with tracer.start_as_current_span("monitor_api_call") as span:
        try:
            data = request.get_json()
            if not data:
                return jsonify({"error": "No data provided"}), 400
            
            # Extract API call data
            endpoint = data.get('endpoint')
            method = data.get('method', 'GET')
            status_code = data.get('status_code', 200)
            duration_ms = data.get('duration_ms', random.randint(10, 1000))
            
            if not endpoint:
                return jsonify({"error": "endpoint is required"}), 400
            
            # Create API call event
            api_call = ApiCallEvent(
                endpoint=endpoint,
                method=method,
                status_code=status_code,
                duration_ms=duration_ms,
                user_id=data.get('user_id'),
                trace_id=data.get('trace_id'),
                span_id=data.get('span_id'),
                error_message=data.get('error_message'),
                request_size_bytes=data.get('request_size_bytes'),
                response_size_bytes=data.get('response_size_bytes'),
                ip_address=request.remote_addr,
                user_agent=request.headers.get('User-Agent')
            )
            
            # Store API call
            api_calls.append(api_call)
            
            # Determine status category
            status_category = get_status_category(status_code)
            
            # Record metrics
            attributes = {
                "endpoint": endpoint,
                "method": method,
                "status_code": str(status_code),
                "status_category": status_category
            }
            
            api_call_counter.add(1, attributes)
            api_duration_histogram.record(duration_ms, attributes)
            
            # Record error metrics if applicable
            if status_code >= 400:
                api_error_counter.add(1, attributes)
            
            # Update endpoint statistics
            if endpoint not in api_endpoints:
                api_endpoints[endpoint] = {
                    'total_calls': 0,
                    'total_errors': 0,
                    'total_duration': 0,
                    'methods': set()
                }
            
            api_endpoints[endpoint]['total_calls'] += 1
            api_endpoints[endpoint]['total_duration'] += duration_ms
            api_endpoints[endpoint]['methods'].add(method)
            
            if status_code >= 400:
                api_endpoints[endpoint]['total_errors'] += 1
            
            # Set span attributes
            span.set_attribute("api.endpoint", endpoint)
            span.set_attribute("api.method", method)
            span.set_attribute("api.status_code", status_code)
            span.set_attribute("api.duration_ms", duration_ms)
            span.set_attribute("api.status_category", status_category)
            
            if api_call.user_id:
                span.set_attribute("user.id", api_call.user_id)
            
            if api_call.error_message:
                span.set_attribute("error.message", api_call.error_message)
            
            logger.info(f"API call monitored: {method} {endpoint} - {status_code} ({duration_ms}ms)")
            
            return jsonify({
                "status": "success",
                "message": "API call monitored",
                "api_call_id": len(api_calls)
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error monitoring API call: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/monitor/batch', methods=['POST'])
def monitor_api_batch():
    """Monitor multiple API calls in batch."""
    
    with tracer.start_as_current_span("monitor_api_batch") as span:
        try:
            data = request.get_json()
            if not data or 'api_calls' not in data:
                return jsonify({"error": "api_calls array is required"}), 400
            
            api_calls_data = data['api_calls']
            processed_count = 0
            
            for call_data in api_calls_data:
                try:
                    # Extract API call data
                    endpoint = call_data.get('endpoint')
                    method = call_data.get('method', 'GET')
                    status_code = call_data.get('status_code', 200)
                    duration_ms = call_data.get('duration_ms', random.randint(10, 1000))
                    
                    if not endpoint:
                        continue
                    
                    # Create API call event
                    api_call = ApiCallEvent(
                        endpoint=endpoint,
                        method=method,
                        status_code=status_code,
                        duration_ms=duration_ms,
                        user_id=call_data.get('user_id'),
                        trace_id=call_data.get('trace_id'),
                        span_id=call_data.get('span_id'),
                        error_message=call_data.get('error_message'),
                        request_size_bytes=call_data.get('request_size_bytes'),
                        response_size_bytes=call_data.get('response_size_bytes'),
                        ip_address=request.remote_addr,
                        user_agent=request.headers.get('User-Agent')
                    )
                    
                    # Store API call
                    api_calls.append(api_call)
                    processed_count += 1
                    
                    # Record metrics
                    status_category = get_status_category(status_code)
                    attributes = {
                        "endpoint": endpoint,
                        "method": method,
                        "status_code": str(status_code),
                        "status_category": status_category
                    }
                    
                    api_call_counter.add(1, attributes)
                    api_duration_histogram.record(duration_ms, attributes)
                    
                    if status_code >= 400:
                        api_error_counter.add(1, attributes)
                    
                except Exception as e:
                    logger.error(f"Error processing API call in batch: {e}")
                    continue
            
            span.set_attribute("batch.size", len(api_calls_data))
            span.set_attribute("batch.processed", processed_count)
            
            logger.info(f"API batch monitored: {processed_count}/{len(api_calls_data)} calls processed")
            
            return jsonify({
                "status": "success",
                "message": "API batch monitored",
                "processed_count": processed_count,
                "total_count": len(api_calls_data)
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error monitoring API batch: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/monitor/simulate', methods=['POST'])
def simulate_api_calls():
    """Simulate API calls for testing."""
    
    with tracer.start_as_current_span("simulate_api_calls") as span:
        try:
            data = request.get_json() or {}
            count = data.get('count', 10)
            
            endpoints = [
                '/api/users',
                '/api/orders',
                '/api/products',
                '/api/payments',
                '/api/auth/login',
                '/api/auth/logout',
                '/api/profile',
                '/api/search'
            ]
            
            methods = ['GET', 'POST', 'PUT', 'DELETE']
            status_codes = [200, 201, 400, 401, 403, 404, 500]
            
            simulated_calls = []
            
            for i in range(count):
                endpoint = random.choice(endpoints)
                method = random.choice(methods)
                status_code = random.choice(status_codes)
                duration_ms = random.randint(10, 2000)
                
                # Create API call event
                api_call = ApiCallEvent(
                    endpoint=endpoint,
                    method=method,
                    status_code=status_code,
                    duration_ms=duration_ms,
                    user_id=f"user_{random.randint(1, 100)}",
                    error_message=f"Error message {i}" if status_code >= 400 else None,
                    ip_address=f"192.168.1.{random.randint(1, 254)}",
                    user_agent=f"DemoAgent/{random.randint(1, 10)}"
                )
                
                # Store API call
                api_calls.append(api_call)
                simulated_calls.append({
                    'endpoint': endpoint,
                    'method': method,
                    'status_code': status_code,
                    'duration_ms': duration_ms
                })
                
                # Record metrics
                status_category = get_status_category(status_code)
                attributes = {
                    "endpoint": endpoint,
                    "method": method,
                    "status_code": str(status_code),
                    "status_category": status_category
                }
                
                api_call_counter.add(1, attributes)
                api_duration_histogram.record(duration_ms, attributes)
                
                if status_code >= 400:
                    api_error_counter.add(1, attributes)
            
            span.set_attribute("simulation.count", count)
            
            logger.info(f"Simulated {count} API calls")
            
            return jsonify({
                "status": "success",
                "message": f"Simulated {count} API calls",
                "simulated_calls": simulated_calls
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error simulating API calls: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/monitor/stats', methods=['GET'])
def get_api_stats():
    """Get API monitoring statistics."""
    
    with tracer.start_as_current_span("get_api_stats") as span:
        try:
            # Calculate statistics
            total_calls = len(api_calls)
            total_errors = sum(1 for call in api_calls if call.status_code >= 400)
            error_rate = (total_errors / total_calls * 100) if total_calls > 0 else 0
            
            # Average duration
            if api_calls:
                avg_duration = sum(call.duration_ms for call in api_calls) / len(api_calls)
            else:
                avg_duration = 0
            
            # Status code distribution
            status_distribution = {}
            for call in api_calls:
                status = call.status_code
                status_distribution[status] = status_distribution.get(status, 0) + 1
            
            # Method distribution
            method_distribution = {}
            for call in api_calls:
                method = call.method
                method_distribution[method] = method_distribution.get(method, 0) + 1
            
            # Endpoint statistics
            endpoint_stats = {}
            for endpoint, stats in api_endpoints.items():
                endpoint_stats[endpoint] = {
                    'total_calls': stats['total_calls'],
                    'total_errors': stats['total_errors'],
                    'error_rate': (stats['total_errors'] / stats['total_calls'] * 100) if stats['total_calls'] > 0 else 0,
                    'avg_duration': stats['total_duration'] / stats['total_calls'] if stats['total_calls'] > 0 else 0,
                    'methods': list(stats['methods'])
                }
            
            span.set_attribute("stats.total_calls", total_calls)
            span.set_attribute("stats.total_errors", total_errors)
            span.set_attribute("stats.error_rate", error_rate)
            
            return jsonify({
                "status": "success",
                "stats": {
                    "total_calls": total_calls,
                    "total_errors": total_errors,
                    "error_rate": round(error_rate, 2),
                    "average_duration_ms": round(avg_duration, 2),
                    "status_distribution": status_distribution,
                    "method_distribution": method_distribution,
                    "endpoint_stats": endpoint_stats
                }
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error getting API stats: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "service": "api-monitor",
        "timestamp": datetime.now().isoformat()
    })

def get_status_category(status_code: int) -> str:
    """Get status category from HTTP status code."""
    if 200 <= status_code < 300:
        return "success"
    elif 300 <= status_code < 400:
        return "redirect"
    elif 400 <= status_code < 500:
        return "client_error"
    elif 500 <= status_code < 600:
        return "server_error"
    else:
        return "unknown"

if __name__ == '__main__':
    port = int(os.getenv('PORT', 8083))
    app.run(host='0.0.0.0', port=port, debug=True)
