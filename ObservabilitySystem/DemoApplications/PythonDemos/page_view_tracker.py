#!/usr/bin/env python3
"""
Page View Tracker Demo
Demonstrates page view tracking with OpenTelemetry instrumentation.
"""

import os
import time
import random
import logging
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
        "service.name": "page-view-tracker",
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
page_view_counter = meter.create_counter(
    name="page.views.total",
    description="Total page views",
    unit="1"
)

page_load_time_histogram = meter.create_histogram(
    name="page.load.time",
    description="Page load time in milliseconds",
    unit="ms"
)

session_duration_histogram = meter.create_histogram(
    name="session.duration",
    description="Session duration in seconds",
    unit="s"
)

# In-memory storage for demo
page_views = []
user_sessions = {}

class PageViewEvent:
    """Page view event model."""
    
    def __init__(self, user_id: str, page_name: str, page_url: str, **kwargs):
        self.user_id = user_id
        self.page_name = page_name
        self.page_url = page_url
        self.timestamp = datetime.now()
        self.load_time_ms = kwargs.get('load_time_ms', random.randint(100, 3000))
        self.referrer = kwargs.get('referrer')
        self.viewport_width = kwargs.get('viewport_width', random.randint(320, 1920))
        self.viewport_height = kwargs.get('viewport_height', random.randint(240, 1080))
        self.user_agent = kwargs.get('user_agent')
        self.ip_address = kwargs.get('ip_address')

@app.route('/api/pageview', methods=['POST'])
def track_page_view():
    """Track a page view event."""
    
    with tracer.start_as_current_span("track_page_view") as span:
        try:
            data = request.get_json()
            if not data:
                return jsonify({"error": "No data provided"}), 400
            
            # Extract page view data
            user_id = data.get('user_id', 'anonymous')
            page_name = data.get('page_name')
            page_url = data.get('page_url')
            
            if not page_name or not page_url:
                return jsonify({"error": "page_name and page_url are required"}), 400
            
            # Create page view event
            page_view = PageViewEvent(
                user_id=user_id,
                page_name=page_name,
                page_url=page_url,
                load_time_ms=data.get('load_time_ms'),
                referrer=data.get('referrer'),
                viewport_width=data.get('viewport_width'),
                viewport_height=data.get('viewport_height'),
                user_agent=request.headers.get('User-Agent'),
                ip_address=request.remote_addr
            )
            
            # Store page view
            page_views.append(page_view)
            
            # Record metrics
            attributes = {
                "page.name": page_name,
                "user.id": user_id,
                "page.url": page_url
            }
            
            page_view_counter.add(1, attributes)
            page_load_time_histogram.record(page_view.load_time_ms, attributes)
            
            # Set span attributes
            span.set_attribute("user.id", user_id)
            span.set_attribute("page.name", page_name)
            span.set_attribute("page.url", page_url)
            span.set_attribute("page.load_time_ms", page_view.load_time_ms)
            span.set_attribute("page.viewport_width", page_view.viewport_width)
            span.set_attribute("page.viewport_height", page_view.viewport_height)
            
            if page_view.referrer:
                span.set_attribute("page.referrer", page_view.referrer)
            
            # Update user session
            update_user_session(user_id, page_view)
            
            logger.info(f"Page view tracked: {page_name} by user {user_id}")
            
            return jsonify({
                "status": "success",
                "message": "Page view tracked",
                "page_view_id": len(page_views)
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error tracking page view: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/session/start', methods=['POST'])
def start_session():
    """Start a new user session."""
    
    with tracer.start_as_current_span("start_session") as span:
        try:
            data = request.get_json()
            user_id = data.get('user_id', 'anonymous')
            session_id = f"session_{user_id}_{int(time.time())}"
            
            user_sessions[session_id] = {
                'user_id': user_id,
                'start_time': datetime.now(),
                'pages_visited': [],
                'ip_address': request.remote_addr,
                'user_agent': request.headers.get('User-Agent')
            }
            
            span.set_attribute("user.id", user_id)
            span.set_attribute("session.id", session_id)
            
            logger.info(f"Session started: {session_id} for user {user_id}")
            
            return jsonify({
                "status": "success",
                "session_id": session_id,
                "message": "Session started"
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error starting session: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/session/end', methods=['POST'])
def end_session():
    """End a user session."""
    
    with tracer.start_as_current_span("end_session") as span:
        try:
            data = request.get_json()
            session_id = data.get('session_id')
            
            if not session_id or session_id not in user_sessions:
                return jsonify({"error": "Session not found"}), 404
            
            session = user_sessions[session_id]
            duration = (datetime.now() - session['start_time']).total_seconds()
            
            # Record session duration
            session_duration_histogram.record(duration, {
                "user.id": session['user_id'],
                "session.id": session_id
            })
            
            span.set_attribute("session.id", session_id)
            span.set_attribute("user.id", session['user_id'])
            span.set_attribute("session.duration_seconds", duration)
            span.set_attribute("session.pages_visited", len(session['pages_visited']))
            
            # Remove session
            del user_sessions[session_id]
            
            logger.info(f"Session ended: {session_id}, duration: {duration:.2f}s")
            
            return jsonify({
                "status": "success",
                "session_id": session_id,
                "duration_seconds": duration,
                "pages_visited": len(session['pages_visited']),
                "message": "Session ended"
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error ending session: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/summary', methods=['GET'])
def get_analytics_summary():
    """Get analytics summary."""
    
    with tracer.start_as_current_span("get_analytics_summary") as span:
        try:
            # Calculate summary statistics
            total_page_views = len(page_views)
            unique_users = len(set(pv.user_id for pv in page_views))
            active_sessions = len(user_sessions)
            
            # Page popularity
            page_counts = {}
            for pv in page_views:
                page_counts[pv.page_name] = page_counts.get(pv.page_name, 0) + 1
            
            popular_pages = sorted(page_counts.items(), key=lambda x: x[1], reverse=True)[:10]
            
            # Average load time
            if page_views:
                avg_load_time = sum(pv.load_time_ms for pv in page_views) / len(page_views)
            else:
                avg_load_time = 0
            
            span.set_attribute("analytics.total_page_views", total_page_views)
            span.set_attribute("analytics.unique_users", unique_users)
            span.set_attribute("analytics.active_sessions", active_sessions)
            
            return jsonify({
                "status": "success",
                "summary": {
                    "total_page_views": total_page_views,
                    "unique_users": unique_users,
                    "active_sessions": active_sessions,
                    "average_load_time_ms": round(avg_load_time, 2),
                    "popular_pages": popular_pages
                }
            })
            
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.Status(trace.StatusCode.ERROR, str(e)))
            logger.error(f"Error getting analytics summary: {e}")
            return jsonify({"error": str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "service": "page-view-tracker",
        "timestamp": datetime.now().isoformat()
    })

def update_user_session(user_id: str, page_view: PageViewEvent):
    """Update user session with page view."""
    # Find active session for user
    for session_id, session in user_sessions.items():
        if session['user_id'] == user_id:
            session['pages_visited'].append({
                'page_name': page_view.page_name,
                'page_url': page_view.page_url,
                'timestamp': page_view.timestamp,
                'load_time_ms': page_view.load_time_ms
            })
            break

if __name__ == '__main__':
    port = int(os.getenv('PORT', 8082))
    app.run(host='0.0.0.0', port=port, debug=True)
