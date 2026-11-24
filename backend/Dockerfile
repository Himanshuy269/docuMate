FROM python:3.11-slim

# Install LibreOffice headless suite, core fonts, and runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    fonts-dejavu-core \
    fonts-liberation \
    default-jre-headless \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Flask for minimal REST service
RUN pip install --no-cache-dir flask==3.1.0 werkzeug==3.1.3

COPY server.py /app/server.py

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENV PYTHONUNBUFFERED=1

CMD ["python", "server.py"]
