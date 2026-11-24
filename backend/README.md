# DocuMate LibreOffice Conversion Backend

This lightweight containerized backend service converts Microsoft Office documents (.docx, .doc, .xlsx, .xls, .pptx, .ppt) to PDF using a headless LibreOffice installation.

---

## Architecture & Privacy Model

- **Isolated Ephemeral Execution**: Each conversion executes inside an isolated container in an ephemeral temporary directory (`tempfile.TemporaryDirectory`).
- **Zero Document Retention**: Input and output files are deleted immediately after the HTTP response stream finishes.
- **REST Contract**:
  - `GET /health`: Health-check endpoint verifying service status.
  - `POST /api/v1/convert`: Multipart form upload with field `file` containing the document bytes. Streams back the converted PDF with `Content-Type: application/pdf`.

---

## Quick Start (Docker)

### 1. Build and Run Container
```bash
cd backend
docker compose up -d --build
```

### 2. Verify Health
```bash
curl http://localhost:8080/health
# Response: {"service":"documate-libreoffice-converter","status":"ok","version":"1.0.0"}
```

### 3. Test Manual Conversion
```bash
curl -X POST -F "file=@sample.docx" http://localhost:8080/api/v1/convert -o sample.pdf
```

---

## Android Connection Configuration

- **Android Emulator**: Set backend URL in DocuMate Settings to `http://10.0.2.2:8080` (alias to host loopback).
- **Physical Device**: Set backend URL to your host machine's local LAN IP (e.g., `http://192.168.1.100:8080`).
