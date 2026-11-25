# DocuMate 📄

> **Privacy-first document toolkit for Android** — convert, compress, and manage your documents entirely on-device, with zero telemetry and zero cloud storage.

---

## ✨ Features

| Feature | Description | Processing |
|---|---|---|
| 🖼️ **Images → PDF** | Combine multiple images into a single PDF | On-device |
| ➕ **Add Images to PDF** | Append or insert images into an existing PDF | On-device |
| 📦 **PDF Compressor** | Reduce PDF file size with configurable quality | On-device |
| 📝 **Word → PDF** | Convert `.docx` / `.doc` to PDF | Backend (LibreOffice) |
| 📊 **Excel → PDF** | Convert `.xlsx` / `.xls` to PDF | Backend (LibreOffice) |
| 📽️ **PowerPoint → PDF** | Convert `.pptx` / `.ppt` to PDF | Backend (LibreOffice) |
| 🕓 **History** | Browse and re-open previously converted files | On-device |
| ⚙️ **Settings** | Configure backend URL, compression level, page size, and theme | On-device |

---

## 🏗️ Architecture

DocuMate follows a **clean, layered architecture** across two main components:

```
DocuMate/
├── app/                        # Android application (Kotlin + Jetpack Compose)
│   └── src/main/java/com/example/documate/
│       ├── core/               # Shared infrastructure
│       │   ├── common/         # AppResult sealed class (success/error)
│       │   ├── data/           # Repository implementations
│       │   ├── di/             # Manual dependency injection (AppContainer)
│       │   ├── domain/         # Models, repository interfaces, shared use-cases
│       │   ├── navigation/     # Compose Navigation setup
│       │   ├── theme/          # Material 3 color, typography, theme
│       │   └── ui/             # Shared reusable composables
│       └── feature/            # One package per screen / feature slice
│           ├── home/
│           ├── images_to_pdf/
│           ├── add_images_to_pdf/
│           ├── pdf_compressor/
│           ├── word_to_pdf/
│           ├── excel_to_pdf/
│           ├── powerpoint_to_pdf/
│           ├── history/
│           └── settings/
└── backend/                    # Python Flask micro-service (LibreOffice conversion)
    ├── server.py
    ├── Dockerfile
    └── docker-compose.yml
```

Each feature slice follows the **UI → ViewModel → UseCase → Repository** chain and owns its own domain models, use-cases, and presentation layer.

---

## 🔒 Privacy & Security

DocuMate is built on a **Privacy-First, Zero-Knowledge** philosophy:

- **On-device by default** — image and PDF operations never leave your phone.
- **Zero telemetry** — no analytics, ad SDKs, or behavioral tracking of any kind.
- **Zero persistent cloud storage** — no remote databases, no user accounts.
- **Least privilege** — no `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`; file access is brokered exclusively via Android's **Storage Access Framework (SAF)**.
- **Secure sharing** — files are shared between apps only through `FileProvider` with temporary read-only URI grants; raw `file://` URIs are never exposed.
- **Explicit consent for remote conversion** — users must actively consent before any Office document bytes are sent to the LibreOffice backend.

See [SECURITY.md](SECURITY.md) for the full threat model and permissions audit.

---

## 🛠️ Tech Stack

### Android App
| Layer | Library / Tool |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| Navigation | AndroidX Navigation Compose |
| Async | KotlinX Coroutines |
| Build | Gradle 9 (Version Catalog) |
| Min SDK | Defined in `app/build.gradle.kts` |

### Backend
| Component | Technology |
|---|---|
| Runtime | Python 3 / Flask |
| Converter | LibreOffice (headless) |
| Container | Docker / Docker Compose |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Ladybug or later
- **JDK 17+**
- **Docker** (optional — only needed for Word / Excel / PowerPoint conversion)

### 1. Clone the repository

```bash
git clone https://github.com/Himanshuy269/docuMate.git
cd docuMate
```

### 2. Open in Android Studio

Open the `DocuMate` root folder in Android Studio. Gradle will sync automatically.

### 3. Run the app

Connect a device or start an emulator, then click **Run ▶** in Android Studio.

> The app is fully functional without the backend — all image and PDF operations work offline.

### 4. (Optional) Start the LibreOffice backend

```bash
cd backend
docker compose up -d --build
```

Verify the backend is running:

```bash
curl http://localhost:8080/health
# {"service":"documate-libreoffice-converter","status":"ok","version":"1.0.0"}
```

Then, in DocuMate **Settings**, set the backend URL to:
- **Android Emulator**: `http://10.0.2.2:8080`
- **Physical device (same LAN)**: `http://<your-host-ip>:8080`

---

## 📡 Backend API

| Endpoint | Method | Description |
|---|---|---|
| `/health` | `GET` | Health check — returns service name and version |
| `/api/v1/convert` | `POST` | Converts an Office document to PDF |

**Convert request:**
```
POST /api/v1/convert
Content-Type: multipart/form-data
Field: file = <document bytes>
```

**Convert response:**
```
200 OK
Content-Type: application/pdf
(PDF bytes streamed inline)
```

---

## 📂 Project Structure Highlights

```
app/src/main/java/com/example/documate/
├── DocuMateApplication.kt      # Application class & AppContainer initialization
├── MainActivity.kt             # Single-activity host
├── core/
│   ├── common/AppResult.kt     # Typed success / error wrapper
│   ├── di/AppContainer.kt      # Manual DI container
│   └── navigation/             # NavHost & route definitions
└── feature/
    └── <feature>/
        ├── domain/usecase/     # Business logic
        └── presentation/       # Screen, ViewModel, UiState, UiEvent
```

---

## 🤝 Contributing

1. Fork the repository and create a feature branch (`git checkout -b feature/my-feature`).
2. Commit your changes with a clear message (`git commit -m "feat: add my feature"`).
3. Push to your fork and open a Pull Request.

Please follow the existing code style (Kotlin Coding Conventions) and ensure no new external analytics or tracking dependencies are introduced.

---

## 🔐 Reporting Security Issues

Please **do not** open public GitHub issues for security vulnerabilities. Instead, email `marvelstudiom64@gmail.com` with details. See [SECURITY.md](SECURITY.md) for the full disclosure policy.

---

## 📄 License

This project is open-source under the MIT License.
