FROM python:3.12-slim

WORKDIR /app

# Install Flask and OpenTelemetry dependencies
RUN pip install --no-cache-dir wrapt==1.15.0 \
    flask==2.3.3

# Copy test application
COPY test_app.py .

# Expose port
EXPOSE 8080

# Run the application
CMD ["python", "test_app.py"]
                                                                                                                                                               