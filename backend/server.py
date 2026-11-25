import os
import subprocess
import tempfile
from flask import Flask, request, send_file, jsonify, render_template_string

app = Flask(__name__)

# Enable CORS for all routes so Web (Wasm) and web clients can access the backend
@app.after_request
def add_cors_headers(response):
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
    return response

@app.route("/", methods=["GET", "OPTIONS"])
def index():
    if request.method == "OPTIONS":
        return "", 204

    # If compiled Wasm distribution exists, serve it
    dist_dir = os.path.join(os.path.dirname(__file__), "..", "composeApp", "build", "dist", "wasmJs", "productionExecutable")
    dist_index = os.path.join(dist_dir, "index.html")
    if os.path.exists(dist_index):
        return send_file(dist_index)

    # Otherwise serve the interactive DocuMate Web Application interface
    html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DocuMate - Privacy-First Document Toolkit</title>
    <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%231E5BB8'><path d='M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z'/></svg>">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #1E5BB8;
            --primary-hover: #174894;
            --primary-light: #EBF2FC;
            --surface: #FFFFFF;
            --background: #F8FAFC;
            --text-primary: #0F172A;
            --text-secondary: #475569;
            --border: #E2E8F0;
            --success: #10B981;
            --error: #EF4444;
            --radius: 14px;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background: var(--background);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        header {
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            padding: 1rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .logo {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            font-weight: 700;
            font-size: 1.25rem;
            color: var(--primary);
            text-decoration: none;
        }
        .badge {
            background: #ECFDF5;
            color: #065F46;
            font-size: 0.75rem;
            font-weight: 600;
            padding: 0.25rem 0.6rem;
            border-radius: 999px;
            display: flex;
            align-items: center;
            gap: 0.35rem;
        }
        .badge::before {
            content: "";
            width: 6px;
            height: 6px;
            background: var(--success);
            border-radius: 50%;
        }
        main {
            flex: 1;
            max-width: 800px;
            width: 100%;
            margin: 2.5rem auto;
            padding: 0 1.5rem;
        }
        .hero {
            text-align: center;
            margin-bottom: 2.5rem;
        }
        .hero h1 {
            font-size: 2.25rem;
            font-weight: 800;
            letter-spacing: -0.025em;
            margin-bottom: 0.5rem;
            color: var(--text-primary);
        }
        .hero p {
            color: var(--text-secondary);
            font-size: 1.05rem;
        }
        .card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 2rem;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -2px rgba(0,0,0,0.05);
        }
        .dropzone {
            border: 2px dashed #CBD5E1;
            border-radius: 12px;
            padding: 3rem 1.5rem;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            background: #F8FAFC;
        }
        .dropzone:hover, .dropzone.dragover {
            border-color: var(--primary);
            background: var(--primary-light);
        }
        .dropzone svg {
            width: 48px;
            height: 48px;
            fill: none;
            stroke: var(--primary);
            stroke-width: 1.5;
            margin-bottom: 1rem;
        }
        .dropzone h3 {
            font-size: 1.1rem;
            font-weight: 600;
            margin-bottom: 0.25rem;
        }
        .dropzone p {
            font-size: 0.875rem;
            color: var(--text-secondary);
        }
        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            background: var(--primary);
            color: white;
            font-weight: 600;
            font-size: 0.95rem;
            padding: 0.8rem 1.5rem;
            border-radius: 10px;
            border: none;
            cursor: pointer;
            width: 100%;
            margin-top: 1.25rem;
            transition: background 0.2s ease;
        }
        .btn:hover:not(:disabled) {
            background: var(--primary-hover);
        }
        .btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
        .file-info {
            display: none;
            margin-top: 1rem;
            padding: 0.85rem 1rem;
            background: #F1F5F9;
            border-radius: 8px;
            align-items: center;
            justify-content: space-between;
            font-size: 0.875rem;
        }
        .file-info span {
            font-weight: 500;
            color: var(--text-primary);
        }
        .alert {
            margin-top: 1rem;
            padding: 0.85rem 1rem;
            border-radius: 8px;
            font-size: 0.875rem;
            display: none;
        }
        .alert-error {
            background: #FEF2F2;
            color: #991B1B;
            border: 1px solid #F87171;
        }
        .alert-success {
            background: #ECFDF5;
            color: #065F46;
            border: 1px solid #6EE7B7;
        }
        .features-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 1.25rem;
            margin-top: 2rem;
        }
        .feature-item {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 1.25rem;
            text-align: center;
        }
        .feature-item .icon {
            font-size: 1.5rem;
            margin-bottom: 0.5rem;
        }
        .feature-item h4 {
            font-size: 0.95rem;
            font-weight: 600;
            margin-bottom: 0.25rem;
        }
        .feature-item p {
            font-size: 0.8rem;
            color: var(--text-secondary);
        }
        footer {
            text-align: center;
            padding: 1.5rem;
            color: var(--text-secondary);
            font-size: 0.85rem;
            border-top: 1px solid var(--border);
            background: var(--surface);
        }
    </style>
</head>
<body>
    <header>
        <a href="/" class="logo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
            </svg>
            DocuMate
        </a>
        <div class="badge" id="backendStatus">Service Ready</div>
    </header>

    <main>
        <div class="hero">
            <h1>Convert Office Documents to PDF</h1>
            <p>Fast, high-fidelity conversion for Word, Excel, and PowerPoint files with zero persistent data storage.</p>
        </div>

        <div class="card">
            <div class="dropzone" id="dropzone">
                <svg viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z"/>
                </svg>
                <h3>Choose a document or drag it here</h3>
                <p>Supports .docx, .doc, .xlsx, .xls, .pptx, .ppt</p>
                <input type="file" id="fileInput" accept=".docx,.doc,.xlsx,.xls,.pptx,.ppt" style="display:none">
            </div>

            <div class="file-info" id="fileInfo">
                <span id="fileName">document.docx</span>
                <span id="fileSize">0 KB</span>
            </div>

            <div class="alert alert-error" id="errorAlert"></div>
            <div class="alert alert-success" id="successAlert"></div>

            <button class="btn" id="convertBtn" disabled>
                <span>Convert to PDF</span>
            </button>
        </div>

        <div class="features-grid">
            <div class="feature-item">
                <div class="icon">🔒</div>
                <h4>Zero Retention</h4>
                <p>Files are converted in ephemeral containers and wiped immediately.</p>
            </div>
            <div class="feature-item">
                <div class="icon">⚡</div>
                <h4>High Fidelity</h4>
                <p>Preserves layouts, formulas, fonts, and graphics accurately.</p>
            </div>
            <div class="feature-item">
                <div class="icon">📱</div>
                <h4>Cross-Platform</h4>
                <p>Accessible from Android, iOS, and modern desktop browsers.</p>
            </div>
        </div>
    </main>

    <footer>
        DocuMate · Privacy-First Document Toolkit · Open Source
    </footer>

    <script>
        const dropzone = document.getElementById('dropzone');
        const fileInput = document.getElementById('fileInput');
        const fileInfo = document.getElementById('fileInfo');
        const fileName = document.getElementById('fileName');
        const fileSize = document.getElementById('fileSize');
        const convertBtn = document.getElementById('convertBtn');
        const errorAlert = document.getElementById('errorAlert');
        const successAlert = document.getElementById('successAlert');

        let selectedFile = null;

        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
        dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files.length > 0) {
                handleFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', () => {
            if (fileInput.files.length > 0) {
                handleFile(fileInput.files[0]);
            }
        });

        function handleFile(file) {
            selectedFile = file;
            fileName.textContent = file.name;
            fileSize.textContent = (file.size / 1024).toFixed(1) + ' KB';
            fileInfo.style.display = 'flex';
            convertBtn.disabled = false;
            errorAlert.style.display = 'none';
            successAlert.style.display = 'none';
        }

        convertBtn.addEventListener('click', async () => {
            if (!selectedFile) return;

            convertBtn.disabled = true;
            convertBtn.innerHTML = '<span>Converting document...</span>';
            errorAlert.style.display = 'none';
            successAlert.style.display = 'none';

            const formData = new FormData();
            formData.append('file', selectedFile);

            try {
                const response = await fetch('/api/v1/convert', {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) {
                    const errData = await response.json().catch(() => ({}));
                    throw new Error(errData.error || ('Server returned HTTP ' + response.status));
                }

                const blob = await response.blob();
                const downloadUrl = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = downloadUrl;
                const base = selectedFile.name.substring(0, selectedFile.name.lastIndexOf('.')) || 'converted';
                a.download = base + '.pdf';
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(downloadUrl);

                successAlert.textContent = 'Conversion successful! Your PDF has been downloaded.';
                successAlert.style.display = 'block';
            } catch (err) {
                errorAlert.textContent = 'Conversion error: ' + err.message;
                errorAlert.style.display = 'block';
            } finally {
                convertBtn.disabled = false;
                convertBtn.innerHTML = '<span>Convert to PDF</span>';
            }
        });

        // Verify backend health
        fetch('/health')
            .then(res => res.json())
            .then(data => {
                if (data.status === 'ok') {
                    document.getElementById('backendStatus').textContent = 'Backend Online';
                }
            })
            .catch(() => {
                document.getElementById('backendStatus').textContent = 'Service Standby';
            });
    </script>
</body>
</html>
"""
    return render_template_string(html_content)

@app.route("/health", methods=["GET", "OPTIONS"])
def health():
    if request.method == "OPTIONS":
        return "", 204
    return jsonify({
        "status": "ok",
        "service": "documate-libreoffice-converter",
        "version": "1.0.0"
    })

@app.route("/api/v1/convert", methods=["POST", "OPTIONS"])
def convert():
    if request.method == "OPTIONS":
        return "", 204
    if "file" not in request.files:
        return jsonify({"error": "No file uploaded in 'file' multipart field"}), 400

    uploaded_file = request.files["file"]
    if not uploaded_file.filename:
        return jsonify({"error": "Empty filename provided"}), 400

    # Target format (default 'pdf', or 'docx', 'xlsx', 'pptx')
    target_format = request.form.get("format") or request.args.get("format") or "pdf"
    target_format = target_format.strip().lower().lstrip(".")

    format_mappings = {
        "pdf": ("pdf", "application/pdf", ".pdf"),
        "docx": ("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
        "xlsx": ("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
        "pptx": ("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
        "doc": ("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
        "xls": ("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
        "ppt": ("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
    }

    if target_format not in format_mappings:
        return jsonify({"error": f"Unsupported target format: {target_format}. Supported formats: pdf, docx, xlsx, pptx"}), 400

    lo_target, mime_type, ext = format_mappings[target_format]

    original_filename = os.path.basename(uploaded_file.filename)
    if not original_filename:
        return jsonify({"error": "Invalid filename provided"}), 400

    base_name = os.path.splitext(original_filename)[0] or "converted"

    with tempfile.TemporaryDirectory() as temp_dir:
        input_path = os.path.join(temp_dir, original_filename)
        uploaded_file.save(input_path)

        if os.path.getsize(input_path) == 0:
            return jsonify({"error": "Uploaded document is empty (0 bytes)"}), 400

        # Execute headless LibreOffice conversion
        cmd = [
            "libreoffice",
            "--headless",
            "--convert-to", lo_target,
            "--outdir", temp_dir,
            input_path
        ]

        try:
            proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=90)
            if proc.returncode != 0:
                err_msg = proc.stderr.decode("utf-8", errors="replace")
                return jsonify({"error": "Conversion process failed", "details": err_msg}), 500

            expected_output = os.path.join(temp_dir, f"{base_name}{ext}")
            if not os.path.exists(expected_output):
                # Search for any produced target file in the temp directory
                matching_files = [f for f in os.listdir(temp_dir) if f.lower().endswith(ext)]
                if matching_files:
                    expected_output = os.path.join(temp_dir, matching_files[0])
                else:
                    return jsonify({"error": f"Output {target_format.upper()} was not generated by LibreOffice"}), 500

            return send_file(
                expected_output,
                mimetype=mime_type,
                as_attachment=True,
                download_name=f"{base_name}{ext}"
            )
        except subprocess.TimeoutExpired:
            return jsonify({"error": "Document conversion timed out after 90 seconds"}), 504
        except Exception as e:
            return jsonify({"error": f"Server error: {str(e)}"}), 500

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port)
