# Wallpaper qualifier

## 0.RawIdea.md

## Main goal

The goal of this app is to use a set of images as samples of wallpapers that the user likes and considers good for the device. The app should load those images and extract as much information as possible from each image. That information should be compiled into a definition of a good wallpaper. From that, another set of images should be loaded and qualified as good or bad wallpapers. The good ones should then be copied into an output folder.

## Technical details

The app should use a command line interface, no UI is required.

It should connect to an external LLM for the required steps. No AI embedded directly into the app. 

The app should run locally on macOS.

The app should be build in Kotlin using Kotlin Multiplatform with macOS as target. Open question: Is Kotlin Koog a good fit for this?

The app should run as many steps in parallel as possible, but only one request must be sent to the LLM at time. No parallel requests to the LLM.

The app must accept as many image formats as possible. Each file must be converted to a widely supported format like jpeg or png for LLM analysis, but the output copies should preserve the original format with best quality.

The app should accept a json file as input. That file should contain things like folders, LLM connection parameters and anything else.

The app must not modify any file or folder structure outside of a designated `temp` folder. The `temp` folder path must be provided as a configuration parameter in the JSON file. Files and folders created within the `temp` directory must be removed as soon as they are no longer needed to minimize disk usage.

## 01.RefinedSpec.md

---

# Wallpaper Selection App - Product Specification

**Version:** 1.0

**Date:** 2026-02-25

**Status:** Draft

---

## Executive Summary

An intelligent wallpaper curation system that learns from user-provided sample images to automatically evaluate and filter wallpaper candidates. The system uses external LLM analysis to extract aesthetic and technical characteristics from sample images, builds a quality profile, and applies that profile to classify new wallpapers as suitable or unsuitable for the user's device.

---

## 1. Product Requirements

### 1.1 Main Goal

The goal of this app is to use a set of images as samples of wallpapers that the user likes and considers good for the device. The app should load those images and extract as much information as possible from each image. That information should be compiled into a definition of a good wallpaper. From that, another set of images should be loaded and qualified as good or bad wallpapers. The good ones should then be copied into an output folder.

### 1.2 User Stories

**US-001: Sample Analysis**

As a user, I want to provide a folder of sample wallpapers I like, so the system can learn what makes a good wallpaper for my device.

**Acceptance Criteria:**

- System accepts a folder path containing sample images
- All common image formats are supported (JPEG, PNG, HEIC, WebP, TIFF, BMP, etc.)
- System processes each sample image and extracts characteristics
- System provides progress feedback during analysis
- Analysis results are stored for reuse

**US-002: Quality Profile Generation**

As a user, I want the system to create a comprehensive quality profile from my samples, so it understands my aesthetic preferences and technical requirements.

**Acceptance Criteria:**

- System compiles characteristics from all sample images
- Profile includes aesthetic elements (color palettes, composition, subject matter, style)
- Profile includes technical requirements (resolution, aspect ratio, quality metrics)
- Profile is human-readable and can be reviewed
- Profile can be saved and reused across sessions

**US-003: Candidate Evaluation**

As a user, I want to provide a folder of candidate wallpapers for evaluation, so the system can identify which ones match my preferences.

**Acceptance Criteria:**

- System accepts a folder path containing candidate images
- Each candidate is evaluated against the quality profile
- System provides a qualification score or binary good/bad classification
- Evaluation rationale is provided for each image
- Progress is reported during batch evaluation

**US-004: Automatic Curation**

As a user, I want qualified wallpapers automatically copied to an output folder, so I have a curated collection ready to use.

**Acceptance Criteria:**

- System copies only "good" wallpapers to specified output folder
- Images are copied in their original format with best quality (lossless where possible)
- Original filenames are preserved or enhanced with metadata
- Duplicate detection prevents redundant copies
- Summary report shows how many images were selected

**US-005: Configuration Management**

As a user, I want to configure the system via a JSON file, so I can customize behavior without modifying code.

**Acceptance Criteria:**

- JSON configuration file specifies all operational parameters
- Configuration includes: sample folder, candidate folder, output folder, temp folder, LLM connection details
- Configuration includes: optimization settings, quality thresholds, parallel processing limits
- Invalid configuration produces clear error messages
- Configuration can be validated before execution
- Temp folder path is validated and must be writable

---

## 2. Technical Requirements

### 2.1 Platform & Runtime

**TR-001: macOS Native Execution**

- System must run locally on macOS (minimum version: [NEEDS CLARIFICATION: macOS 12+, 13+, or 14+?])
- No server or cloud dependencies for core functionality
- All processing happens on local machine

**TR-002: Command-Line Interface**

- Application operates entirely through CLI
- No graphical user interface required
- Standard input/output for all interactions
- Exit codes follow Unix conventions (0 = success, non-zero = error)

**TR-002a: Temporary File Isolation**

- Application must not modify any file or folder structure outside a designated `temp` folder
- `temp` folder path must be specified in the JSON configuration file under `folders.temp`
- All temporary files, intermediate processing outputs, and scratch data must reside within the `temp` folder
- Files and folders created within `temp` must be removed as soon as they are no longer needed
- Only the final qualified images in their original format should be written to the configured output folder
- Original sample and candidate images must never be modified under any circumstances

**TR-003: Kotlin Multiplatform Implementation**

- Built using Kotlin Multiplatform with macOS as target
- [NEEDS CLARIFICATION: Kotlin/Native for macOS or Kotlin/JVM with macOS-specific bindings?]
- [NEEDS CLARIFICATION: Is Kotlin Koog (KOOG) framework suitable for this use case? Requires research into KOOG's image processing and file I/O capabilities]

### 2.2 External Dependencies

**TR-004: LLM Integration**

- System connects to external LLM service (no embedded AI)
- [NEEDS CLARIFICATION: Which LLM service(s)? OpenAI GPT-4 Vision, Anthropic Claude 3, Google Gemini Vision, or configurable?]
- LLM handles image analysis and quality assessment
- Connection parameters (API endpoint, authentication, model selection) specified in configuration
- Graceful error handling for LLM service failures

**TR-005: Sequential LLM Requests**

- Only one request sent to LLM at a time
- No parallel LLM requests under any circumstances
- Request queue manages sequential processing
- [NEEDS CLARIFICATION: Should system retry failed LLM requests? If so, how many attempts?]

### 2.3 Image Processing

**TR-006: Format Support**

- Accept maximum range of image formats: JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW formats
- [NEEDS CLARIFICATION: Which RAW formats? CR2, NEF, ARW, DNG?]
- Automatic format detection (not relying solely on file extension)

**TR-007: Image Conversion for Analysis**

- Convert images to widely supported format (JPEG or PNG) for LLM analysis
- This conversion is temporary and used only for processing
- Preserve aspect ratio during conversion
- Original format images are copied to output folder without modification

### 2.4 Performance & Concurrency

**TR-008: Parallel Processing**

- Run as many steps in parallel as possible
- Parallel operations: file I/O, image decoding, format conversion, optimization
- Sequential operations: LLM requests only
- [NEEDS CLARIFICATION: Maximum parallel thread count, or unlimited based on system resources?]

**TR-009: Resource Management**

- Efficient memory usage for large image batches
- [NEEDS CLARIFICATION: Should system process images in batches to limit memory usage?]
- Progress reporting doesn't significantly impact performance

---

## 3. Functional Specifications

### 3.1 Configuration Schema

**JSON Configuration Structure:**

```json
{
  "folders": {
    "samples": "/path/to/sample/wallpapers",
    "candidates": "/path/to/candidate/wallpapers",
    "output": "/path/to/output/wallpapers",
    "temp": "/path/to/temp/folder"
  },
  "llm": {
    "provider": "openai|anthropic|google",
    "apiKey": "your-api-key",
    "endpoint": "https://api.provider.com/v1",
    "model": "gpt-4-vision|claude-3-opus|gemini-pro-vision",
    "timeout": 30,
    "retryAttempts": 3
  },
  "processing": {
    "maxParallelTasks": 8,
    "outputFormat": "jpeg|png|auto",
    "jpegQuality": 85,
    "maxFileSize": 2097152,
    "qualityThreshold": 0.7
  },
  "options": {
    "saveProfile": true,
    "profilePath": "/path/to/profile.json",
    "verbose": true,
    "skipDuplicates": true
  }
}
```

### 3.2 Processing Workflow

**Phase 1: Initialization**

1. Load and validate configuration file
2. Verify folder paths exist and are accessible
3. Validate LLM connection
4. Initialize processing queue

**Phase 2: Sample Analysis**

1. Discover all image files in samples folder
2. For each sample image (in parallel where possible):
- Load and decode image
- Convert to LLM-compatible format
- Queue for LLM analysis
1. Send images to LLM sequentially
2. Extract characteristics from LLM responses
3. Compile aggregated quality profile

**Phase 3: Profile Generation**

1. Analyze all sample characteristics
2. Identify common patterns and preferences
3. Generate quality criteria document
4. Save profile if configured
5. Display profile summary to user

**Phase 4: Candidate Evaluation**

1. Discover all image files in candidates folder
2. For each candidate image (in parallel where possible):
- Load and decode image
- Convert to LLM-compatible format
- Queue for LLM evaluation
1. Send images to LLM sequentially with quality profile context
2. Receive qualification assessment
3. Track qualified vs. rejected images

**Phase 5: Curation**

1. For each qualified image (in parallel):
- Retrieve original image file
- Check for duplicates in output folder
- Copy to output folder with original format and best quality
1. Generate summary report
2. Display results to user

### 3.3 LLM Interaction Patterns

**Sample Analysis Prompt Template:**

```other
Analyze this wallpaper image and extract the following characteristics:
- Primary colors and color palette
- Composition style (minimalist, busy, centered, rule-of-thirds, etc.)
- Subject matter (abstract, nature, architecture, etc.)
- Visual style (photographic, illustrated, geometric, etc.)
- Technical quality (resolution, sharpness, noise level)
- Aspect ratio and orientation
- Mood and aesthetic (calm, energetic, dark, bright, etc.)

Provide structured output in JSON format.
```

**Candidate Evaluation Prompt Template:**

```other
Given this quality profile of preferred wallpapers:
[PROFILE DATA]

Evaluate this candidate wallpaper image and determine if it matches the profile.
Provide:
- Binary qualification (good/bad)
- Confidence score (0.0 to 1.0)
- Reasoning for the decision
- Specific matches or mismatches with profile criteria

Provide structured output in JSON format.
```

---

## 4. Non-Functional Requirements

### 4.1 Performance

**NFR-001: Processing Speed**

- Sample analysis: [NEEDS CLARIFICATION: Target time per image? E.g., "under 5 seconds per image"]
- Candidate evaluation: [NEEDS CLARIFICATION: Target throughput? E.g., "100 images in under 10 minutes"]
- Image optimization: [NEEDS CLARIFICATION: Target time? E.g., "under 1 second per image"]

**NFR-002: Resource Usage**

- Memory: [NEEDS CLARIFICATION: Maximum memory footprint? E.g., "under 2GB for 1000 images"]
- CPU: Utilize available cores efficiently without starving system
- Disk: Temporary files cleaned up after processing

### 4.2 Reliability

**NFR-003: Error Handling**

- Graceful handling of corrupted image files
- Recovery from LLM service interruptions
- Clear error messages with actionable guidance
- Partial results preserved if process is interrupted

**NFR-004: Data Integrity**

- Original images never modified
- Atomic file operations (no partial writes)
- Verification of copied files
- All temporary files remain isolated within the `temp` folder
- Application does not modify any file or folder structure outside the `temp` folder (except for the final output folder where qualified images are copied)

### 4.3 Usability

**NFR-005: Progress Reporting**

- Real-time progress updates during long operations
- Percentage completion for batch operations
- Estimated time remaining
- Clear indication of current phase

**NFR-006: Output Clarity**

- Human-readable summary reports
- Structured logs for debugging
- Clear distinction between informational and error messages

### 4.4 Maintainability

**NFR-007: Code Quality**

- Modular architecture with clear separation of concerns
- Comprehensive unit tests for core logic
- Integration tests for LLM interaction
- Documentation for all public APIs

**NFR-008: Extensibility**

- Easy to add support for new LLM providers
- Pluggable image processing pipeline
- Configurable evaluation criteria

---

## 5. Constraints & Assumptions

### 5.1 Constraints

**C-001: LLM Dependency**

- System requires active internet connection for LLM access
- LLM service costs are user's responsibility
- LLM rate limits may affect processing speed

**C-002: Platform Limitation**

- macOS only (no Windows or Linux support in initial version)
- Requires macOS system libraries for image processing

**C-003: Sequential LLM Processing**

- LLM request serialization may create bottleneck for large batches
- Processing time scales linearly with number of images

**C-004: Temp Folder Isolation**

- Application must have write and delete permissions on the temp folder
- Temp folder must be on a filesystem that supports atomic operations
- Insufficient disk space in temp folder will cause processing failures
- Temp folder cleanup may be delayed if files are locked by system processes

### 5.2 Assumptions

**A-001: Sample Quality**

- User provides representative sample set (minimum 5-10 images recommended)
- Samples reflect actual preferences

**A-002: LLM Capabilities**

- LLM can accurately analyze visual characteristics
- LLM provides consistent evaluations across similar images

**A-003: File System**

- User has read access to sample and candidate folders
- User has write access to output folder
- User has write and delete access to temp folder
- Sufficient disk space available for temp files and output copies
- Temp folder path is accessible and not a system-protected directory

---

## 6. Success Criteria

### 6.1 Functional Success

- System successfully analyzes sample images and generates quality profile
- System accurately evaluates candidate images (>80% user agreement with selections)
- Qualified images are correctly copied in original format with best quality
- Configuration file controls all operational parameters

### 6.2 Technical Success

- Application runs stably on macOS without crashes
- LLM requests are strictly sequential
- Parallel processing improves overall throughput
- All supported image formats are handled correctly

### 6.3 User Success

- User can run entire workflow with single command
- Progress is clearly visible throughout processing
- Results are immediately usable (qualified images in original format with best quality)
- Configuration is intuitive and well-documented

---

## 7. Out of Scope (Initial Version)

- Graphical user interface
- Support for video wallpapers
- Real-time wallpaper rotation/scheduling
- Cloud storage integration
- Mobile device support
- Batch configuration management
- Machine learning model training (relying on external LLM only)
- Image editing or enhancement features
- Wallpaper preview or gallery view

---

## 8. Open Questions & Research Items

### 8.1 Technical Research Required

**R-001: Kotlin Koog Framework Evaluation**

- Is Kotlin Koog suitable for this use case?
- Does it provide adequate image processing capabilities?
- What are the file I/O performance characteristics?
- Are there better alternatives for Kotlin/Native on macOS?

**R-002: Image Processing Libraries**

- Which Kotlin-compatible image processing library for macOS?
- Options: Skiko (Skia for Kotlin), native macOS APIs via cinterop, JVM-based libraries
- Performance comparison needed

**R-003: LLM Provider Selection**

- Which LLM provider offers best vision capabilities for this use case?
- Cost comparison for typical usage (e.g., 100 images)
- Rate limits and batch processing implications

### 8.2 Clarifications Needed from Stakeholder

**Q-001:** Minimum macOS version requirement?

**Q-002:** Preferred LLM provider(s) or should it be configurable?

**Q-003:** Output image format preference (JPEG, PNG, or automatic)?

**Q-004:** Target file size or quality level for optimization?

**Q-005:** Maximum parallel thread count or system-determined?

**Q-006:** Should system batch-process images to limit memory usage?

**Q-007:** LLM request retry policy (attempts, backoff strategy)?

**Q-008:** Performance targets (time per image, throughput)?

**Q-009:** Memory usage constraints?

**Q-010:** Minimum recommended sample set size?

---

## 9. Next Steps

### 9.1 Immediate Actions

1. **Research Phase**
- Evaluate Kotlin Koog framework capabilities
- Research Kotlin-compatible image processing libraries for macOS
- Compare LLM provider vision APIs (capabilities, cost, rate limits)
- Prototype LLM prompt templates with sample images
1. **Clarification Phase**
- Review open questions with stakeholder
- Finalize technical requirements based on research findings
- Define specific performance targets
1. **Design Phase**
- Create detailed architecture document
- Design data models (configuration schema, profile schema, result schema)
- Define API contracts for internal modules
- Plan test strategy

### 9.2 Implementation Phases

**Phase 1: Foundation** (Estimated: 1-2 weeks)

- Project setup and build configuration
- Configuration file parsing and validation
- Basic CLI interface and argument handling
- Logging and error handling framework

**Phase 2: Image Processing** (Estimated: 1-2 weeks)

- Image loading and format detection
- Format conversion pipeline
- Optimization and compression
- File I/O operations

**Phase 3: LLM Integration** (Estimated: 1 week)

- LLM client implementation
- Request queue and sequential processing
- Prompt template system
- Response parsing and validation

**Phase 4: Core Logic** (Estimated: 2 weeks)

- Sample analysis workflow
- Profile generation algorithm
- Candidate evaluation workflow
- Curation and copying logic

**Phase 5: Polish & Testing** (Estimated: 1 week)

- Comprehensive testing (unit, integration, end-to-end)
- Performance optimization
- Documentation
- User acceptance testing

---

## 10. Appendix

### 10.1 Glossary

- **Sample Images**: User-provided wallpapers that represent desired aesthetic and quality
- **Quality Profile**: Aggregated characteristics and criteria derived from sample analysis
- **Candidate Images**: Wallpapers to be evaluated against the quality profile
- **Qualification**: Binary or scored assessment of whether a candidate matches the profile
- **Curation**: Process of copying qualified images to output folder in original format with best quality
- **LLM**: Large Language Model with vision capabilities (e.g., GPT-4 Vision, Claude 3)

### 10.2 References

- Kotlin Multiplatform Documentation: [https://kotlinlang.org/docs/multiplatform.html](https://kotlinlang.org/docs/multiplatform.html)
- Kotlin/Native for macOS: [https://kotlinlang.org/docs/native-overview.html](https://kotlinlang.org/docs/native-overview.html)
- Spec-Driven Development Methodology: [https://github.com/github/spec-kit](https://github.com/github/spec-kit)
- OpenAI Vision API: [https://platform.openai.com/docs/guides/vision](https://platform.openai.com/docs/guides/vision)
- Anthropic Claude Vision: [https://docs.anthropic.com/claude/docs/vision](https://docs.anthropic.com/claude/docs/vision)

### 10.3 Document History

| **Version** | **Date**   | **Author**      | **Changes**                                      |
| ----------- | ---------- | --------------- | ------------------------------------------------ |
| 1.0         | 2026-02-25 | Craft Assistant | Initial specification based on user requirements |

</Content