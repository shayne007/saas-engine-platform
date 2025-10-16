#!/usr/bin/env python3
# custom_configurator.py - Custom configuration for OpenTelemetry auto-instrumentation

import os
import logging
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.instrumentor import BaseInstrumentor

# Import custom extensions
try:
    from custom_extensions.custom_span_processor import CustomSpanProcessor
except ImportError:
    CustomSpanProcessor = None

logger = logging.getLogger(__name__)

class CustomConfigurator:
    """Custom configurator for OpenTelemetry auto-instrumentation"""
    
    @staticmethod
    def configure_tracing():
        """Configure custom tracing with OpenTelemetry"""
        # Get environment variables
        service_name = os.environ.get("OTEL_SERVICE_NAME", "unknown-service")
        endpoint = os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")
        environment = os.environ.get("DEPLOYMENT_ENVIRONMENT", "development")
        
        # Create resource with custom attributes
        resource = Resource.create({
            "service.name": service_name,
            "deployment.environment": environment,
            "custom.attribute": "custom-auto-instrumentation"
        })
        
        # Create tracer provider with resource
        tracer_provider = TracerProvider(resource=resource)
        
        # Create OTLP exporter
        otlp_exporter = OTLPSpanExporter(endpoint=endpoint)
        
        # Add custom span processor if available
        if CustomSpanProcessor:
            logger.info("Adding custom span processor")
            tracer_provider.add_span_processor(CustomSpanProcessor())
        
        # Add batch span processor with OTLP exporter
        tracer_provider.add_span_processor(BatchSpanProcessor(otlp_exporter))
        
        # Set global tracer provider
        trace.set_tracer_provider(tracer_provider)
        
        logger.info(f"Custom tracing configured for service: {service_name}")
        return tracer_provider

    @staticmethod
    def instrument_all():
        """Instrument all available libraries"""
        from opentelemetry.instrumentation.auto_instrumentation import AutoInstrumentation
        
        # Get list of instrumentations to disable
        disabled_instrumentations = os.environ.get("OTEL_PYTHON_DISABLED_INSTRUMENTATIONS", "")
        disabled_list = [i.strip() for i in disabled_instrumentations.split(",") if i.strip()]
        
        logger.info(f"Disabled instrumentations: {disabled_list}")
        
        # Initialize auto-instrumentation
        auto_instrumentation = AutoInstrumentation()
        auto_instrumentation.instrument()
        
        logger.info("Auto-instrumentation completed")

# Initialize when imported
def initialize():
    """Initialize the custom configurator"""
    logging.basicConfig(level=logging.INFO)
    logger.info("Initializing custom auto-instrumentation")
    
    configurator = CustomConfigurator()
    configurator.configure_tracing()
    configurator.instrument_all()
    
    logger.info("Custom auto-instrumentation initialized successfully")

# Auto-initialize when module is loaded
if __name__ == "__main__":
    initialize()