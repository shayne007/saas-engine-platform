#!/usr/bin/env python3
# custom_span_processor.py - Custom span processor for OpenTelemetry

import logging
from opentelemetry.sdk.trace import ReadableSpan
from opentelemetry.sdk.trace.export import SpanExporter, SpanExportResult
from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter
from opentelemetry.sdk.trace.export import SpanProcessor

logger = logging.getLogger(__name__)

class CustomSpanProcessor(SpanProcessor):
    """
    Custom span processor that adds additional attributes to spans
    and provides custom filtering capabilities.
    """
    
    def __init__(self):
        self._exporter = InMemorySpanExporter()
        logger.info("CustomSpanProcessor initialized")
    
    def on_start(self, span, parent_context=None):
        """Called when a span starts"""
        # Add custom attributes to the span
        span.set_attribute("custom.processed", True)
        span.set_attribute("custom.processor.version", "1.0.0")
        
        # Add service-specific attributes if available
        if hasattr(span, "resource") and span.resource:
            service_name = span.resource.attributes.get("service.name")
            if service_name:
                span.set_attribute("custom.service", service_name)
    
    def on_end(self, span):
        """Called when a span ends"""
        if not isinstance(span, ReadableSpan):
            return
        
        # Apply custom filtering logic
        if self._should_process_span(span):
            # Add additional end-time attributes
            span.set_attribute("custom.processed.time", span.end_time)
            
            # Process the span (in a real implementation, this might send to a custom backend)
            self._exporter.export([span])
    
    def _should_process_span(self, span):
        """Custom logic to determine if a span should be processed"""
        # Example: Only process spans that took longer than 100ms
        duration = (span.end_time - span.start_time) / 1_000_000  # Convert to milliseconds
        if duration > 100:
            span.set_attribute("custom.slow_operation", True)
            return True
        
        # Example: Always process spans with error
        if span.status.status_code != 0:  # Non-zero status code indicates error
            span.set_attribute("custom.error_processed", True)
            return True
            
        # Default processing for other spans
        return True
    
    def shutdown(self):
        """Shuts down the span processor"""
        self._exporter.shutdown()
        
    def force_flush(self, timeout_millis=30000):
        """Force flush the processor"""
        self._exporter.force_flush(timeout_millis)