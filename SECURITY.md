# Security Policy & Architecture Specification

## 1. Security Philosophy & Privacy-First Architecture

**DocuMate** is engineered with a strict **Privacy-First, Zero-Knowledge** architecture. Users handle sensitive, confidential, and proprietary documents (financial records, legal contracts, identity documents, and business presentations). Therefore, the application is designed so that document contents remain under the sole ownership and control of the end-user.

### Core Tenets
1. **On-Device First**: All image-to-PDF generation, page insertion, and PDF compression pipelines execute 100% on-device utilizing Android platform APIs and memory-safe Kotlin pipelines.
2. **Zero Telemetry / Zero Advertising**: DocuMate contains **zero** third-party tracking, behavioral profiling, analytics, or advertising SDKs.
3. **Zero Persistent Cloud Storage**: DocuMate operates without external cloud accounts, remote user databases, or persistent server-side file retention.
4. **Least Privilege**: The application operates without requesting broad file system permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`).
5. **Explicit Informed Consent**: Any operation that requires remote backend processing (such as LibreOffice document parsing) demands explicit, per-session user consent before any bytes leave the local device.

---

## 2. Threat Model & Risk Assessment

| Threat Vector | Potential Impact | Severity | Mitigation in DocuMate |
| :--- | :--- | :--- | :--- |
| **Malformed Document Input (OOM / DoS)** | Denial of service, application crash | Medium | `inJustDecodeBounds` dimension sampling, progressive downsampling, capped bitmap memory footprint. |
| **ZIP Bomb / XML Entity Expansion (XXE)** | High memory consumption, host exhaustion in Office files | High | Local pre-validation; backend LibreOffice sandbox with strict memory limits and execution timeout. |
| **Data Leakage via Cloud Backup** | Exposure of user documents via unencrypted cloud synchronization | High | `android:allowBackup="false"` in `AndroidManifest.xml` prevents OS-level ADB/Drive cloud backups. |
| **File URI Exposure Vulnerability** | Path traversal or exposure of app-private sandbox files | High | All inter-app sharing exclusively uses `androidx.core.content.FileProvider` with temporary read-only URI grants. |
| **Man-in-the-Middle (MitM) on Remote Backend** | Eavesdropping on office document transmission | Critical | Production builds enforce HTTPS/TLS 1.3 endpoints. Endpoints are user-configurable. |
| **Persistent Data Residue on Backend** | Data retained on conversion server after processing | Critical | Ephemeral in-memory handling; temporary disk buffers are purged inside `finally` blocks upon completion. |

---

## 3. Storage Architecture & Sandboxing

DocuMate strictly abides by Android's Scoped Storage guidelines:

### Sandbox Isolation
- **App-Private Directories**: All generated and temporary documents reside in internal storage:
  - Cache: `context.cacheDir/documate_temp/` (ephemeral input copies, scratch bitmaps)
  - Generated PDFs: `context.filesDir/generated_pdfs/`
  - Office PDFs: `context.filesDir/office_pdfs/`
  - Compressed PDFs: `context.filesDir/compressed_pdfs/`
  - Exports: `context.filesDir/exports/`
- Other applications installed on the system cannot inspect, read, or modify these files.

### Storage Access Framework (SAF)
- File selection utilizes `ActivityResultContracts.GetContent()`, allowing the Android system file picker to broker read access for only the user-selected document.
- Exporting documents utilizes `ActivityResultContracts.CreateDocument("application/pdf")`, writing directly to the destination chosen by the user without requiring external storage access permissions.

### Secure File Sharing (`FileProvider`)
- Sharing documents to external viewer apps is mediated through `androidx.core.content.FileProvider`.
- Authority: `${applicationId}.fileprovider`
- Permissions: `Intent.FLAG_GRANT_READ_URI_PERMISSION` only.
- Raw `file://` URIs are never exposed, eliminating `FileUriExposedException` and sandbox escape vectors.

### Data Lifecycle & Cleanup
- Temporary files generated during conversion are automatically purged after pipeline completion.
- A manual **Clear Temporary Cache** option in Settings gives users immediate control to reclaim space and erase any residual artifacts.

---

## 4. Encryption & Protection Mechanisms

- **At-Rest Protection**: Local documents benefit from Android's hardware-backed **File-Based Encryption (FBE)** (Credential Encrypted storage for post-unlock user files). DocuMate intentionally avoids custom, unverified cryptographic wrappers ("fake encryption") and relies on vetted OS-level cryptographic primitives.
- **In-Transit Protection**: Network communication with the user's conversion backend enforces **TLS 1.2 / TLS 1.3** encryption. Cleartext HTTP is discouraged in production.
- **No Illusion of Absolute Security**: DocuMate acknowledges that HTTPS alone does not protect locally stored files if a device's OS-level security is compromised, and neither ProGuard nor R8 makes an Android client application unhackable. Security is achieved through defense-in-depth, least-privilege scoping, and zero data hoarding.

---

## 5. Network Security & Office Conversion Backend

### Local vs. Remote Separation
- **Local Features**: Images to PDF, Add Images to PDF, PDF Compression do not utilize network access.
- **Office Conversion (Word, Excel, PowerPoint)**: Modern `.docx`, `.xlsx`, and `.pptx` formats contain complex proprietary formatting, equations, and layouts that require an isolated rendering backend (LibreOffice).

### Zero-Trust Backend Contract
1. **Explicit User Consent**: The user must actively review and check the disclosure checkbox before any conversion request can be dispatched.
2. **Configurable Endpoint**: Users can connect DocuMate to their own self-hosted LibreOffice server instance (e.g., in a private corporate intranet or local Docker container) via Settings.
3. **No Database / No Storage**: The backend server receives the file via a streaming multipart HTTP POST, executes LibreOffice in headless conversion mode:
   ```bash
   soffice --headless --convert-to pdf --outdir <temp_dir> <input_file>
   ```
4. **Immediate Unlink**: The generated PDF is streamed back to the mobile client, and all source/target files are deleted immediately in a `try ... finally` block.

---

## 6. Permissions Audit

DocuMate requests the minimum viable privileges required for its functionality:

| Permission | Status | Justification |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | Optional/Network | Used exclusively for communicating with the user-configured LibreOffice conversion backend. |
| `android.permission.READ_EXTERNAL_STORAGE` | **NOT REQUESTED** | Replaced entirely with Storage Access Framework (SAF). |
| `android.permission.WRITE_EXTERNAL_STORAGE` | **NOT REQUESTED** | Replaced entirely with Storage Access Framework (SAF). |
| `android.permission.MANAGE_EXTERNAL_STORAGE` | **NOT REQUESTED** | DocuMate does not require broad file access. |

---

## 7. Third-Party Dependencies & Supply Chain Integrity

DocuMate maintains a minimal dependency surface to reduce supply chain attack vectors:

- **Jetpack Compose & Material 3** (Google AndroidX): Standard modern Android UI toolkit.
- **KotlinX Coroutines** (JetBrains): Asynchronous execution and structured concurrency.
- **AndroidX Navigation & Lifecycle**: Architectural state management.
- **JUnit 4** (Testing): Unit testing framework.

No closed-source commercial analytics, binary ad networks, or invasive SDKs are included.

---

## 8. Known Limitations

1. **Password-Protected Documents**: Encrypted Office documents or password-locked PDFs cannot be converted without first removing the password.
2. **Extremely Large Files (> 100 MB)**: Operations on very large documents are bounded by the device's heap allocation limits.
3. **Complex Macro Execution**: LibreOffice conversion ignores embedded VBA macros to prevent remote code execution vectors.

---

## 9. Security Testing Checklist

- [x] **SAF Verification**: App functions without granting `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`.
- [x] **FileProvider Path Isolation**: FileProvider only exposes paths within designated application-private subdirectories.
- [x] **OOM Defense**: 108MP camera photos downsample safely using `inJustDecodeBounds` without throwing `OutOfMemoryError`.
- [x] **User Consent Enforcement**: Office conversions abort immediately with an error if user consent is not actively toggled.
- [x] **Backup Exclusion**: `android:allowBackup="false"` verified in `AndroidManifest.xml`.
- [x] **Cache Eviction**: Scratch files are deleted in `finally` blocks on both success and failure states.

---

## 10. Vulnerability Disclosure & Reporting

We take the security of DocuMate seriously. If you discover a security vulnerability, please report it responsibly:

1. **Email**: Contact the security maintainers at `marvelstudiom64@gmail.com`.
2. **Details to Include**:
   - Description of the vulnerability and potential impact.
   - Minimal reproduction steps, proof-of-concept document, or code snippet.
   - Device model, Android OS version, and DocuMate build version.
3. **Response Timeline**:
   - Acknowledgment: Within 48 hours.
   - Triage & Assessment: Within 5 business days.
   - Patch Release: Within 14 days of validation.

Please do **not** open public GitHub issues for undisclosed security vulnerabilities.
