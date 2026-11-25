# DocuMate 📄

> **Privacy-first cross-platform document toolkit for Android, iOS, and Web (Wasm)** — convert, compress, and manage your documents with Compose Multiplatform, zero telemetry, and zero persistent cloud storage.

---

## ✨ Features

| Feature | Description | Processing | Platform Availability |
|---|---|---|---|
| 🖼️ **Images → PDF** | Combine multiple images into a single PDF | On-device / Client-side | Android • iOS • Web |
| ➕ **Add Images to PDF** | Append or insert images into an existing PDF | On-device / Client-side | Android • iOS • Web |
| 📦 **PDF Compressor** | Reduce PDF file size with configurable quality | On-device / Client-side | Android • iOS • Web |
| 📝 **Word → PDF** | Convert `.docx` / `.doc` to PDF | Backend (LibreOffice) | Android • iOS • Web |
| 📊 **Excel → PDF** | Convert `.xlsx` / `.xls` to PDF | Backend (LibreOffice) | Android • iOS • Web |
| 📽️ **PowerPoint → PDF** | Convert `.pptx` / `.ppt` to PDF | Backend (LibreOffice) | Android • iOS • Web |
| 🕓 **History** | Browse and re-open previously converted files | On-device / LocalStorage | Android • iOS • Web |
| ⚙️ **Settings** | Configure backend URL, compression level, page size, and theme | On-device / LocalStorage | Android • iOS • Web |

---

## 🏗️ Architecture

DocuMate is architected as a modern **Kotlin Multiplatform (KMP) & Compose Multiplatform (CMP)** application sharing 100% of its UI, ViewModels, business logic, domain models, and navigation across Android, iOS, and Web (WebAssembly):

```
DocuMate/
├── composeApp/                       # Shared Compose Multiplatform Module
│   ├── build.gradle.kts              # Multiplatform Gradle configuration
│   └── src/
│       ├── commonMain/               # 100% Shared UI & Business Logic
│       │   └── kotlin/com/example/documate/
│       │       ├── App.kt            # Shared App Composable root
│       │       ├── core/
│       │       │   ├── common/       # AppResult, UUID, DateTime utilities
│       │       │   ├── di/           # Multiplatform AppContainer & DI
│       │       │   ├── domain/       # DocumentFile, PlatformFile, Models, Repository interfaces
│       │       │   ├── navigation/   # Compose Multiplatform Navigation (Screen, NavGraph)
│       │       │   ├── theme/        # Material 3 Color, Shape, Typography, Theme
│       │       │   └── ui/           # Shared composables, expect/actual PlatformFileHandler
│       │       └── feature/          # Feature slices (UI, ViewModels, UseCases)
│       │           ├── home/
│       │           ├── images_to_pdf/
│       │           ├── add_images_to_pdf/
│       │           ├── pdf_compressor/
│       │           ├── word_to_pdf/
│       │           ├── excel_to_pdf/
│       │           ├── powerpoint_to_pdf/
│       │           ├── history/
│       │           └── settings/
│       ├── androidMain/              # Android Target Implementation
│       │   ├── AndroidManifest.xml   # Manifest & FileProvider setup
│       │   ├── res/                  # Android drawables, mipmaps, strings, themes
│       │   └── kotlin/com/example/documate/
│       │       ├── MainActivity.kt   # ComponentActivity entrypoint
│       │       ├── DocuMateApplication.kt
│       │       └── core/data/repository/ # Android SAF, native PdfDocument engine, SharedPreferences
│       ├── iosMain/                  # iOS Target Implementation
│       │   └── kotlin/com/example/documate/
│       │       ├── MainViewController.kt # UIViewController entrypoint
│       │       └── core/data/repository/ # Apple Foundation, PDFKit, NSUserDefaults
│       └── wasmJsMain/               # Web Target Implementation (Kotlin/Wasm)
│           ├── resources/index.html  # HTML5 Canvas container
│           └── kotlin/com/example/documate/
│               ├── main.kt           # ComposeViewport entrypoint
│               └── core/data/repository/ # Browser DOM, File API, LocalStorage
├── iosApp/                           # Native Xcode Project
│   ├── iosApp/
│   │   ├── iOSApp.swift              # SwiftUI App entrypoint
│   │   ├── ContentView.swift         # View hosting MainViewController
│   │   └── Info.plist
│   └── iosApp.xcodeproj/            # Xcode Project configured with Kotlin framework
├── backend/                          # Python Flask micro-service (LibreOffice conversion)
│   ├── server.py
│   ├── Dockerfile
│   └── docker-compose.yml
└── gradle/
    └── libs.versions.toml            # Version Catalog with CMP & Multiplatform dependencies
```

---

## 🔒 Privacy & Security

DocuMate is built on a **Privacy-First, Zero-Knowledge** philosophy:

- **On-device & Client-side first** — image and PDF operations occur directly on the client without sending data to servers.
- **Zero telemetry** — no analytics, ad SDKs, or behavioral tracking across all platforms.
- **Zero persistent cloud storage** — no remote databases, no user accounts required.
- **Cross-platform sandbox isolation** — platform-specific storage frameworks (Android SAF, iOS Sandbox Documents, Web LocalStorage).
- **Explicit consent for remote conversion** — users must actively consent before Office document bytes are sent to the LibreOffice conversion backend.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.1 Multiplatform |
| **UI Framework** | JetBrains Compose Multiplatform 1.7 (Material 3) |
| **Supported Platforms** | **Android** (SDK 26+), **iOS** (iOS 15+), **Web** (Wasm-JS) |
| **Navigation** | `org.jetbrains.androidx.navigation:navigation-compose` |
| **State Management** | Multiplatform `ViewModel` (`lifecycle-viewmodel-compose`), `StateFlow`, `Coroutines` |
| **Networking** | Ktor Client (CIO on Android, Darwin on iOS, JS on Web) |
| **Backend Engine** | Python 3 / Headless LibreOffice in Docker |

---

## 🚀 Running the App

### 1. Android
To run the Android application:
```bash
./gradlew :composeApp:installDebug
```
Or open the project in **Android Studio** and click **Run 'composeApp'**.

### 2. iOS
Open `iosApp/iosApp.xcodeproj` in **Xcode** (on macOS) and click **Run**, or build the framework via Gradle:
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### 3. Web (Wasm)
To start the local development development web server with live reloading:
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
To build production Web static assets:
```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
The output HTML and WebAssembly bundles will be generated under `composeApp/build/dist/wasmJs/productionExecutable`.

### 4. Optional LibreOffice Backend (for Word, Excel, PowerPoint)
```bash
cd backend
docker-compose up --build
```
Server listens on `http://localhost:8080` (or `http://10.0.2.2:8080` for Android Emulator).
