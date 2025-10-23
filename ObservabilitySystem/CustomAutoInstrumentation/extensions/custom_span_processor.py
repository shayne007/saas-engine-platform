#!/usr/bin/env python3
# custom_span_processor.py - Custom span processor for OpenTelemetry

import logging
from opentelemetry.sdk.trace import ReadableSpan
from opentelemetry.sdk.trace.export import SpanExporter, SpanExportResult
from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter
from opentelemetry.sdk.trace.export import SpanProcessor
import json

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
        try:
            # Add custom attributes to the span
            span.set_attribute("custom.processed", True)
            span.set_attribute("custom.processor.version", "1.0.0")
            # span.set_attribute("custom.starttime", span.start_time)
            # span.set_attribute("custom.endtime", span.end_time) # no end time for active span
            # Add service-specific attributes if available with robust error handling
            try:
                if hasattr(span, "resource") and span.resource:
                    if hasattr(span.resource, "attributes"):
                        logger.info(f"Span resource attributes when on_start: {span.resource.attributes}")
                        service_name = span.resource.attributes.get("service.name")
                        if service_name:
                            span.set_attribute("custom.service", service_name)
            except Exception as e:
                logger.warning(f"Error accessing span resource attributes: {e}")
        except Exception as e:
            logger.error(f"Error in on_start method: {e}")
    
    def on_end(self, span):
        """Called when a span ends"""
        try:
            if not isinstance(span, ReadableSpan):
                return
            
            # Apply custom filtering logic
            try:
                if self._should_process_span(span):
                    span_dict = {
                        "name": span.name,
                        "start_time": span.start_time,
                        "end_time": span.end_time,
                        "status": str(span.status.status_code) if hasattr(span, "status") and span.status else None,
                        "attributes": dict(span.attributes) if hasattr(span, "attributes") and span.attributes else {},
                        "resource": dict(span.resource.attributes) if hasattr(span, "resource") and span.resource and hasattr(span.resource, "attributes") else {}
                    }
                    logger.info(json.dumps(span_dict))
                    # Process the span (in a real implementation, this might send to a custom backend)
                    # Note: We can't modify ReadableSpan attributes, so we just export it
                    self._exporter.export([span])
            except Exception as e:
                logger.error(f"Error in span filtering: {e}")
                # Still try to export the span even if filtering fails
                try:
                    self._exporter.export([span])
                except Exception as export_error:
                    logger.error(f"Error exporting span: {export_error}")
        except Exception as e:
            logger.error(f"Error in on_end method: {e}")
    
    def _should_process_span(self, span):
        """Custom logic to determine if a span should be processed"""
        try:
            # Calculate duration with error handling
            try:
                logger.info(f"Span start time: {span.start_time}")
                logger.info(f"Span end time: {span.end_time}")
                duration = (span.end_time - span.start_time) / 1_000_000  # Convert to milliseconds
                
                # Log slow operations instead of modifying the span
                if duration > 100:
                    logger.info(f"Slow operation detected: {span.name} took {duration}ms")
            except Exception as duration_error:
                logger.warning(f"Error calculating span duration: {duration_error}")
            
            # Log errors instead of modifying the span
            try:
                if hasattr(span, "status") and span.status and hasattr(span.status, "status_code"):
                    if span.status.status_code != 0:  # Non-zero status code indicates error
                        span_name = span.name if hasattr(span, "name") else "unknown"
                        logger.info(f"Error detected in span: {span_name}")
            except Exception as status_error:
                logger.warning(f"Error checking span status: {status_error}")
                
            # Process all spans
            return True
        except Exception as e:
            logger.error(f"Error in _should_process_span method: {e}")
            return True  # Default to processing the span even if errors occur
    
    def shutdown(self):
        """Shuts down the span processor"""
        try:
            if hasattr(self, '_exporter') and self._exporter:
                self._exporter.shutdown()
                logger.info("CustomSpanProcessor shutdown complete")
        except Exception as e:
            logger.error(f"Error during processor shutdown: {e}")
        
    def force_flush(self, timeout_millis=30000):
        """Force flush the processor"""
        try:
            if hasattr(self, '_exporter') and self._exporter:
                return self._exporter.force_flush(timeout_millis)
            return 0  # Success
        except Exception as e:
            logger.error(f"Error during force flush: {e}")
            return 0  # Default to success to avoid propagating errors