FROM python:3.11-slim

WORKDIR /app

# Install Flask
RUN pip install --no-cache-dir flask==2.3.3

# Copy test application
COPY test_app.py .

# Expose port
EXPOSE 8080

# Run the application
CMD ["python", "test_app.py"]
                                                                                                                                                               