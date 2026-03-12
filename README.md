# 🖼️ Wallpaper Qualifier

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Java 21](https://img.shields.io/badge/java-21-red.svg?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Wallpaper Qualifier** is a high-performance macOS command-line tool that leverages local AI to curate your wallpaper collection. It learns your aesthetic preferences from a set of "sample" images and automatically evaluates new candidates, copying those that meet your quality standards to an output directory.

By using local LLMs (via [LMStudio](https://lmstudio.ai/)), your data remains private while benefiting from state-of-the-art multimodal image analysis.

---

## ✨ Key Features

-   **🧠 Personalized Learning:** Automatically builds a "Quality Profile" based on your existing favorite wallpapers.
-   **🤖 AI-Powered Analysis:** Uses local multimodal LLMs for deep aesthetic and technical evaluation.
-   **⚡ Parallel Processing:** Efficiently handles image I/O and format conversion using up to 8 parallel threads.
-   **🔒 Privacy First:** All processing happens locally. No images are ever uploaded to the cloud.
-   **📸 Extensive Format Support:** Supports JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, and RAW formats.
-   **🛠️ Safe & Clean:** Never modifies original images; uses an isolated temporary folder for all processing.

---

## 📋 Prerequisites

-   **macOS:** Optimized for macOS 13+.
-   **Java 21:** Required to build and run the application.
-   **LMStudio:** A local LLM service running on port 1234 (default).
    -   Must have a **multimodal model** loaded (e.g., Llama 3 Vision, Moondream, etc.).

---

## 🚀 Quick Start

### 1. Build the Project

Clone the repository and build the shadow JAR using Gradle:

```bash
./gradlew shadowJar
```

The executable JAR will be located at `build/libs/wallpaper-qualifier-1.0.0-all.jar`.

### 2. Prepare Your Folders

-   `samples/`: Place images that represent your ideal wallpaper style.
-   `candidates/`: Place new images you want to evaluate.
-   `output/`: Qualified images will be copied here.

### 3. Configure the Tool

Create a `config.json` file in the root directory:

```json
{
  "folders": {
    "samples": "./samples",
    "candidates": "./candidates",
    "output": "./output",
    "temp": "./temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "your-multimodal-model-id"
  },
  "processing": {
    "maxParallelTasks": 8,
    "confidenceThreshold": 0.75
  }
}
```

### 4. Run Evaluation

```bash
java -jar build/libs/wallpaper-qualifier-1.0.0-all.jar config.json
```

---

## 💻 CLI Usage

```bash
./wallpaper-qualifier [config-file] [flags]
```

| Flag | Short | Description |
| :--- | :--- | :--- |
| `--help` | `-h` | Show help and usage information |
| `--version` | `-v` | Show current version |
| `--verbose` | `-V` | Enable detailed debug logging |

---

## 📂 Project Structure

```text
.
├── src/main/kotlin/com/wallpaperqualifier/
│   ├── cli/         # Argument parsing and USAGE definitions
│   ├── config/      # Configuration loading and validation
│   ├── domain/      # Core data models and Result types
│   ├── image/       # Format detection and processing
│   ├── llm/         # LMStudio API client and prompt management
│   ├── workflow/    # High-level curation logic
│   └── App.kt       # Application entry point
├── docs/            # Technical specifications and implementation roadmap
├── build.gradle.kts # Build configuration
└── README.md        # This file
```

---

## 📖 Documentation

-   [Configuration Guide](CONFIGURATION.md) - Full details on `config.json` options.
-   [CLI Usage Details](CLI_USAGE.md) - Advanced command-line usage.
-   [Troubleshooting](TROUBLESHOOTING.md) - Common issues and fixes.
-   [Implementation Breakdown](docs/implementation-breakdown/README.md) - Detailed project roadmap.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
