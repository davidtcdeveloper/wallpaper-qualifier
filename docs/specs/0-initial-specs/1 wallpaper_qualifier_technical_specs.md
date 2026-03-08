# Wallpaper Qualifier: Technical Specifications and Open Questions

## Table of Contents

1. [Technical Requirements](#technical-requirements)
  - [Platform & Runtime](#platform--runtime)
  - [External Dependencies](#external-dependencies)
  - [Image Processing](#image-processing)
  - [Performance & Concurrency](#performance--concurrency)
  - [Non-Functional Requirements](#non-functional-requirements)
2. [Open Technical Questions & Research Items](#open-technical-questions--research-items)
  - [Technical Research Required](#technical-research-required)
  - [Clarifications Needed from Stakeholders](#clarifications-needed-from-stakeholders)
3. [Summary of Alternatives and Recommendations](#summary-of-alternatives-and-recommendations)
4. [Next Steps](#next-steps)

---

## Technical Requirements

### Platform & Runtime

- **TR-001: macOS Native Execution**
  - Must run locally on macOS (minimum version: **macOS 26**).
  - No cloud dependencies for core functionality.
  - All processing occurs on the local machine.
- **TR-002: Command-Line Interface**
  - Entirely CLI-based, no GUI.
  - Standard I/O for interactions.
  - Unix-style exit codes (0 = success, non-zero = error).
- **TR-003: Kotlin Multiplatform Implementation**
  - Built using **Kotlin Multiplatform** targeting macOS.
  - **Clarification needed**: Kotlin/Native for macOS or Kotlin/JVM with macOS bindings?

### External Dependencies

- **TR-004: LLM Integration**
  - Connects to an **external LLM**; user selects any LLM provider supported by Kotlin Koog.
  - Sequential LLM requests only (no parallelism).
  - No retry strategy for failed LLM requests (first version).

### Image Processing

- **TR-006: Format Support**
  - Supports: JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, and RAW formats (e.g., CR2, NEF, ARW, DNG).
  - **Clarification needed**: Which RAW formats to prioritize?
- **TR-007: Image Optimization**
  - Converts images to JPEG/PNG.
  - **Clarification needed**: User preference for output format or automatic selection?
  - Reduces file size while preserving quality.
  - **Clarification needed**: Target file size or quality level (e.g., "under 2MB" or "JPEG quality 85").

### Performance & Concurrency

- **TR-008: Parallel Processing**
  - Parallelizes file I/O, image decoding, format conversion, and optimization.
  - **Clarification needed**: Maximum parallel thread count or system-determined?
- **TR-009: Resource Management**
  - Efficient memory usage for large batches.
  - **Clarification needed**: Should the system batch-process images to limit memory usage?

### Non-Functional Requirements

- **NFR-001: Processing Speed**
  - No performance target for the first version.
- **NFR-002: Resource Usage**
  - **Clarification needed**: Maximum memory footprint (e.g., "under 2GB for 1000 images").
- **NFR-003: Error Handling**
  - Graceful handling of corrupted images and LLM interruptions.
  - Clear, actionable error messages.
- **NFR-004: Data Integrity**
  - Original images remain unmodified.
  - Atomic file operations (no partial writes).

---

## Open Technical Questions & Research Items

### Technical Research Required

- **R-001: Kotlin Koog Framework Evaluation**
  - **Kotlin Koog** is used for image processing and file I/O.
  - **Alternatives**: Skiko (Skia for Kotlin), native macOS APIs via `cinterop`, or JVM-based libraries (e.g., Apache Commons Imaging, TwelveMonkeys).
- **R-002: Image Processing Libraries**
  - **Options**: Kotlin Koog, Skiko, native macOS APIs, or JVM libraries.
  - **Selected**: **Kotlin Koog**.
- **R-003: LLM Provider Selection**
  - User selects any LLM provider; no specific recommendation. Any provider supported by Kotlin Koog is acceptable.

### Clarifications Needed from Stakeholders


| Question ID | Question                                                      | Suggested Alternatives/Clarifications                                                             | Selected approach |
| ----------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ----------------- |
| Q-001       | Minimum macOS version requirement?                            | **Suggestion**: Target **macOS 13+** for broader compatibility.                                   | **macOS 26+**     |
| Q-002       | Preferred LLM provider(s) or configurable?                    | **Suggestion**: Make it configurable via JSON to support multiple providers.                      | User selects any LLM supported by Koog |
| Q-003       | Output image format preference (JPEG, PNG, or automatic)?     | **Suggestion**: Default to **PNG** for lossless quality, with JPEG as an option.                  | Default to **PNG**, with JPEG as an option. |
| Q-004       | Target file size or quality level for optimization?           | **Suggestion**: Aim for **"under 2MB"** with **"JPEG quality 85"** as a baseline.                 | **"under 2MB"** with **"JPEG quality 85"** as baseline. |
| Q-005       | Maximum parallel thread count or system-determined?           | **Suggestion**: Use **system-determined** threads with a configurable cap (e.g., 8 threads).      | **System-determined** threads with configurable cap (e.g., 8). |
| Q-006       | Should the system batch-process images to limit memory usage? | **Suggestion**: Yes, batch-process in chunks of **100 images** to balance speed and memory usage. | Yes, **100 images** per batch. |
| Q-007       | LLM request retry policy (attempts, backoff strategy)?        | **Suggestion**: **3 retries** with exponential backoff (1s, 3s, 9s).                              | **No retry strategy** |
| Q-008       | Performance targets (time per image, throughput)?             | **Suggestion**: **<5s per image**, **100 images in <10 minutes**.                                 | **None for first version** |
| Q-009       | Memory usage constraints?                                     | **Suggestion**: **<2GB for 1000 images**.                                                         | **<2GB for 1000 images**. |
| Q-010       | Minimum recommended sample set size?                          | **Suggestion**: **5-10 images** for a robust quality profile.                                     | **5-10 images**. |


---

## Summary of Alternatives and Recommendations


| **Area**                | **Open Question**         | **Alternatives**                                              | **Recommendation**                                              | **Selected approach** |
| ----------------------- | ------------------------- | ------------------------------------------------------------- | --------------------------------------------------------------- | --------------------- |
| **Framework**           | Kotlin Koog suitability   | Kotlin Koog, Skiko, native macOS APIs, JVM libraries           | Use **Kotlin Koog** for image processing and file I/O.          | **Kotlin Koog**       |
| **Image Processing**    | Library selection         | Kotlin Koog, Skiko, native APIs, Apache Commons Imaging, TwelveMonkeys     | **Kotlin Koog** for compatibility.                      | **Kotlin Koog**       |
| **LLM Provider**        | Provider selection        | Any provider supported by Koog                                | No specific recommendation; user selects any supported provider. | **User choice (any supported by Koog)** |
| **Output Format**       | JPEG vs. PNG              | Configurable, default to PNG                                  | Default to **PNG**, offer JPEG as an option.                    | Default to **PNG**, offer JPEG as an option. |
| **Parallel Processing** | Thread count              | System-determined or capped                                   | **System-determined with a cap of 8 threads**.                  | **System-determined with a cap of 8 threads**. |
| **Batch Processing**    | Memory management         | Batch size for processing                                     | **100 images per batch** to balance speed and memory.           | **100 images per batch**. |
| **Retry Policy**        | LLM request retries       | Number of attempts and backoff strategy                       | No retries (first version).                                     | **No retry strategy** |
| **Performance Targets** | Time per image/throughput | User-defined or default targets                               | None for first version.                                         | **None for first version** |


---

## Next Steps

1. **Finalize Technical Requirements**: Clarify open questions with stakeholders.
2. **Prototype Core Components**: Test Kotlin Koog for image processing; LLM integration via any provider supported by Koog.
3. **Document Configuration**: Define JSON schema for user-configurable parameters.

