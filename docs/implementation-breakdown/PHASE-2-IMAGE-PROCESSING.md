# PHASE 2: IMAGE PROCESSING

**Duration:** ~8-10 days  
**Effort:** 60-80 person-hours  
**Tasks:** 10 major deliverables  
**Status:** Ready to execute after PHASE 1

**Dependency:** PHASE 1 must be complete

---

## Overview

Phase 2 implements image handling: format detection, loading, temporary format conversion for LLM analysis, and parallel file I/O operations. This phase establishes the image pipeline that feeds into LLM analysis (PHASE 3).

**Critical Success Factor:** Comprehensive format support (8+ formats) and robust handling of corrupted images. This is a high-risk area due to format complexity.

**Parallel Execution:** PHASE 2 and PHASE 3 can start simultaneously once PHASE 1 is complete.

---

## Task 1: Kotlin Koog Image Loading Prototype (CRITICAL)

**Objective:** Verify Kotlin Koog can load all required image formats

**Priority:** **HIGHEST** — Must complete before full implementation

**Deliverables:**
- Proof-of-concept: Load JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF with Kotlin Koog
- Metadata extraction: width, height, color depth
- Error handling for unsupported or corrupted images
- Performance baseline for format conversion

**Risk Mitigation:**
- Kotlin Koog may not support all formats natively
- Consider fallback: Use macOS native APIs (Cocoa, Image I/O framework)
- RAW format support (CR2, NEF, ARW, DNG) may require external library

**Success Criteria:**
- At least 8 formats load successfully
- Image dimensions and properties extracted
- Corrupted files don't crash (handled gracefully)
- Performance acceptable (<1s per image)

**Implementation Notes:**
```kotlin
// Pseudo-code structure
object ImageLoaderProto {
    fun loadImage(path: String): Result<ImageMetadata> {
        return try {
            val bufferedImage = ImageIO.read(File(path))
            // Extract metadata
            Success(ImageMetadata(width, height, format, ...))
        } catch (e: Exception) {
            Failure(ImageProcessingException("Failed to load: ${e.message}"))
        }
    }
}
```

**If Koog Insufficient:**
- Fall back to macOS `Image I/O` framework via Kotlin/Native
- Consider `ImageMagick` or `libvips` bindings
- Implement separate loaders per format as needed

**Estimated Effort:** 2-3 days (includes research and prototyping)

---

## Task 2: Image Format Detection & Validation

**Objective:** Reliably identify image format from file headers and extensions

**Deliverables:**
- Enum: `ImageFormat` (JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW, UNKNOWN)
- Format detection by file extension (fast path)
- Format detection by magic bytes/file headers (robust path)
- Validation that file is readable and not corrupted
- Rejection of unsupported formats with clear messaging

**Supported Formats:**
- JPEG (.jpg, .jpeg)
- PNG (.png)
- HEIC (.heic, .heif)
- WebP (.webp)
- TIFF (.tif, .tiff)
- BMP (.bmp)
- GIF (.gif)
- RAW (.cr2, .nef, .arw, .dng)

**Success Criteria:**
- All formats correctly identified
- Corrupted files detected
- Clear error for unsupported formats
- Performance: <10ms per file detection

**Implementation Notes:**
```kotlin
enum class ImageFormat {
    JPEG, PNG, HEIC, WEBP, TIFF, BMP, GIF, RAW, UNKNOWN
}

object FormatDetector {
    fun detectFormat(path: String): Result<ImageFormat> {
        // Check magic bytes first
        // Fall back to extension if needed
    }
}
```

---

## Task 3: Image Loader Service

**Objective:** Discover and load images from folders

**Deliverables:**
- `ImageLoader` service: discovers images recursively in folder
- Filters by format (only supported formats)
- Returns list of `Image` objects with metadata
- Handles nested directories
- Progress reporting for folder traversal
- Error collection (corrupted files logged, processing continues)

**Success Criteria:**
- Discovers all images in folder (including subdirs)
- Unsupported formats skipped with warning
- Corrupted files skipped with error log
- Returns metadata for valid images
- Scales to 10,000+ images

**Implementation Notes:**
```kotlin
class ImageLoader(private val logger: Logger) {
    fun discoverImages(folderPath: String): Result<List<Image>> {
        // Recursively find images
        // Filter by supported format
        // Load metadata
        // Return list or failure if folder doesn't exist
    }
}
```

---

## Task 4: Temporary Format Conversion Pipeline

**Objective:** Convert images to JPEG/PNG for LLM analysis (stored in temp folder only)

**Deliverables:**
- Converter: any format → JPEG or PNG
- Configurable output format (JPEG quality: 70-95)
- Preserves aspect ratio
- Images stored in temp folder with unique names
- Cleanup routine (delete temp after use)
- Error handling (conversion failure → skip image)

**Critical Constraint:**
- **Original images never touched**
- **Output goes to temp folder ONLY**
- Temp images deleted immediately after LLM analysis

**Success Criteria:**
- All formats convert to JPEG/PNG successfully
- Aspect ratio preserved
- File size reasonable (<2 MB per image)
- Temp files cleaned up
- No temp files left behind on error

**Implementation Notes:**
```kotlin
class ImageConverter(private val tempFolder: String) {
    fun convertToJpegForLLM(image: Image): Result<File> {
        // Load image
        // Convert to JPEG (temp folder)
        // Return File path or failure
    }
}
```

**Performance Consideration:**
- Conversion is I/O and CPU intensive
- Parallelize across thread pool (up to 8 threads)
- Monitor memory usage (each conversion may consume 100MB+)

---

## Task 5: Parallel File I/O Coordinator

**Objective:** Manage parallel image processing with thread pool

**Deliverables:**
- Thread pool: configurable size (default 8, max 8)
- Coordinator: distributes tasks (load, decode, convert) across threads
- Handles backpressure (if queue grows too large, pause)
- Progress tracking
- Error collection and recovery

**Critical Constraint:**
- **Max 8 threads** (system-determined with configurable cap)
- **Batch size: 100 images** (prevents memory explosion)

**Success Criteria:**
- All images processed in parallel
- Thread pool respects 8-thread limit
- Batch processing works correctly
- Memory usage stays <2 GB for 1000 images
- Progress reported smoothly

**Implementation Notes:**
```kotlin
class FileIOCoordinator(private val maxThreads: Int = 8) {
    fun processImagesInBatches(
        images: List<Image>,
        batchSize: Int = 100,
        processor: suspend (Image) -> Result<ProcessedImage>
    ): Result<List<ProcessedImage>> {
        // Process in batches
        // Use coroutine pool with max threads
        // Collect and return results
    }
}
```

---

## Task 6: Duplicate Detection System

**Objective:** Identify duplicate or very similar images

**Deliverables:**
- Duplicate detection mechanism (perceptual hash or file hash)
- Comparison: within same folder and across folders
- Configurable sensitivity
- Fast comparison for large batches

**Two Approaches:**
1. **File Hash (Simple):** SHA-256 of file content (fast, exact duplicates only)
2. **Perceptual Hash (Robust):** pHash or aHash (detects similar images, slight edits)

**Recommendation:** Start with file hash; add perceptual hash in PHASE 5 if needed

**Success Criteria:**
- Identical files detected correctly
- Same image at different quality levels detected (if using pHash)
- Performance: <10ms per comparison
- False positive rate <1%

**Implementation Notes:**
```kotlin
class DuplicateDetector {
    fun calculateHash(imagePath: String): Result<String> {
        // SHA-256 or perceptual hash
    }
    
    fun isDuplicate(hash1: String, hash2: String, threshold: Float = 0.95f): Boolean {
        // Compare hashes
    }
}
```

---

## Task 7: Image Metadata Extraction

**Objective:** Extract technical properties from images

**Deliverables:**
- Resolution (width, height)
- Aspect ratio
- Color depth
- DPI (if available)
- Bit depth (8-bit, 16-bit, 24-bit, etc.)
- Orientation (EXIF)
- File size
- Modification date

**Success Criteria:**
- All properties extracted correctly
- Aspect ratio calculated accurately
- EXIF orientation handled
- Works across all 8+ formats

**Implementation Notes:**
```kotlin
data class ImageMetadata(
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val colorDepth: Int,
    val dpi: Int,
    val bitDepth: Int,
    val orientation: ImageOrientation,
    val fileSize: Long,
    val modificationDate: Long
)
```

---

## Task 8: Temp File Manager

**Objective:** Create and clean up temporary files safely

**Deliverables:**
- Create temp folder structure if missing
- Generate unique filenames for temp images
- Track created files
- Cleanup routine (immediate deletion or batch)
- Error handling (don't crash if temp delete fails)
- Logging of temp operations (DEBUG mode)

**Critical Constraint:**
- **Temp files deleted immediately after use**
- **NO lingering temp files**

**Success Criteria:**
- Temp folder created if missing
- Temp files organized (unique names)
- Cleanup removes all temp files
- No temp files left on crashes
- Logging tracks temp operations

**Implementation Notes:**
```kotlin
class TempFileManager(private val tempFolder: String) {
    fun createTempImage(originalName: String): Result<File> {
        // Generate unique temp filename
        // Return temp file path
    }
    
    fun cleanup() {
        // Delete all tracked temp files
        // Log results
    }
}
```

---

## Task 9: Error Recovery for Corrupted Images

**Objective:** Gracefully handle and skip corrupted images

**Deliverables:**
- Detection of corrupted images (can't load, invalid format)
- Logging of corruption (which file, which error)
- Processing continues (skip corrupted, process rest)
- User-friendly error messages
- Summary: X images corrupted, Y processed

**Success Criteria:**
- Corrupted images don't crash process
- Errors logged with file path
- Processing completes with partial results
- Summary report shows what failed

**Implementation Notes:**
- Wrap each image operation in try-catch
- Collect errors, don't throw
- Report at end: "Processed 95 images, 5 corrupted (logged in output)"

---

## Task 10: Progress Reporting for Image Processing

**Objective:** Show user feedback during long image processing

**Deliverables:**
- Progress meter: "Processing images: 42/100 (42%)"
- Batch indicators: "Batch 1/10: Loading images..."
- Format indicators: "Converting to JPEG (8/42)..."
- ETA calculation (if possible)
- Clear messages for different phases

**Success Criteria:**
- Progress updates every 1-2 seconds
- ETA visible and reasonably accurate
- Handles variable processing speeds
- Works with both fast and slow operations

**Implementation Notes:**
```kotlin
class ProgressReporter {
    fun report(current: Int, total: Int, phase: String) {
        val percent = (current * 100) / total
        logger.info("$phase: $current/$total ($percent%)")
    }
}
```

---

## Integration Points

- **PHASE 1:** Uses Logger, Result types, Config
- **PHASE 3:** Provides converted images for LLM analysis
- **PHASE 4:** Receives image processing results

---

## Completion Checklist

Before moving to PHASE 4, verify:

- [ ] Kotlin Koog prototype successful (all 8+ formats load)
- [ ] Format detection working reliably
- [ ] ImageLoader discovers all images
- [ ] Temp conversion produces valid JPEG/PNG
- [ ] Parallel I/O respects 8-thread limit
- [ ] Duplicate detection accurate
- [ ] Metadata extracted correctly
- [ ] Temp files cleaned up
- [ ] Corrupted images handled gracefully
- [ ] Progress reporting clear

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Koog insufficient for all formats | Prototype early (Task 1); use macOS APIs as fallback |
| Format conversion slow | Profile and optimize; batch processing for parallelism |
| Memory explosion with large images | Implement 100-image batch limits; monitor heap |
| Temp folder cleanup failure | Don't fail process; log and continue |
| Duplicate detection false positives | Start with file hash; use pHash only if needed |

---

## Phase 2 Complete Indicators

✅ **Phase 2 is complete when:**
1. All 10 tasks delivered
2. All 8+ image formats load successfully
3. Parallel I/O working efficiently
4. Temp files managed correctly
5. Corrupted images handled gracefully
6. Ready for integration with PHASE 3 (LLM)

---

**Next:** Proceed to [PHASE-4-CORE-LOGIC.md](./PHASE-4-CORE-LOGIC.md) once both PHASE 2 and PHASE 3 complete.
