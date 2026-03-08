# TROUBLESHOOTING & COMMON ISSUES

**Guide to resolving problems during implementation**

---

## Build & Compilation Issues

### Issue: Kotlin Koog Not Available for KMP

**Error:**
```
Could not find com.aallam.koog:koog-core
```

**Root Causes:**
- Koog may not have stable KMP support
- Maven repository doesn't have version
- Network connectivity issue

**Solutions:**
1. Check Koog release status on GitHub/Maven
2. Use alternative: `kotlinx-coroutines` for concurrency (already chosen)
3. Implement HTTP client directly (recommended for LMStudio)

**Fallback Strategy:**
```kotlin
// If Koog unavailable, use direct HTTP + Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest")
implementation("io.ktor:ktor-client-core:latest")
// Remove Koog dependency
```

### Issue: macOS-Specific Dependencies Not Available

**Error:**
```
Native library not found for macOS target
```

**Root Causes:**
- Image processing library not compiled for macOS
- Missing platform-specific code

**Solutions:**
1. Use pure Kotlin alternatives (slower, but compatible)
2. Implement macOS-specific code in `src/macosMain/`
3. Use Skiko or native macOS frameworks for image processing

**Fallback Strategy:**
```
If format conversion slow:
  1. Profile bottleneck
  2. Consider using macOS Image I/O framework directly
  3. Write Kotlin/Native wrapper for Objective-C APIs
```

### Issue: Gradle Build Hangs or Times Out

**Error:**
```
BUILD HANGS (no output for 5+ minutes)
```

**Root Causes:**
- Dependency download stuck
- Network connectivity
- Gradle daemon memory issue

**Solutions:**
1. Kill Gradle daemon: `./gradlew --stop`
2. Add network timeout: `org.gradle.timeout=120`
3. Increase Gradle memory: `export GRADLE_OPTS="-Xmx2g"`
4. Clear Gradle cache: `rm -rf ~/.gradle`

---

## Image Processing Issues

### Issue: Kotlin Koog Cannot Load All Formats

**Error:**
```
java.lang.UnsupportedOperationException: Format not supported
```

**Root Causes:**
- Koog missing codec for format (HEIC, WebP, RAW)
- Library compiled without format support
- macOS-specific format needs native binding

**Solutions:**
1. **For HEIC:** Use macOS Image I/O framework (native support)
2. **For WebP:** Check libwebp availability
3. **For RAW:** Consider external library (libtiff, libraw)
4. **Fallback:** Skip unsupported formats with warning

**Implementation:**
```kotlin
enum class ImageFormat {
    JPEG, PNG, HEIC, WEBP, TIFF, BMP, GIF, RAW, UNSUPPORTED
}

class ImageLoader {
    fun loadImage(path: String): Result<Image> {
        return try {
            val format = detectFormat(path)
            when (format) {
                ImageFormat.HEIC -> loadViaImageIO(path)
                ImageFormat.RAW -> loadViaRAWDecoder(path)
                else -> loadViaKoog(path)
            }
        } catch (e: UnsupportedOperationException) {
            Failure(ImageProcessingException("Format not supported: ${e.message}"))
        }
    }
}
```

### Issue: Image Conversion Too Slow

**Error:**
```
Processing 100 images takes >10 minutes
```

**Root Causes:**
- Sequential conversion (should be parallel)
- Inefficient image codec
- Memory swapping (too many threads)

**Solutions:**
1. Profile: `time ./gradlew run --args="profile.json"`
2. Check thread pool size (should be 8)
3. Reduce batch size if memory usage high
4. Optimize JPEG quality (70-80 sufficient)

**Debug Code:**
```kotlin
class PerformanceProfiler {
    fun profileConversion(imagePath: String) {
        val start = System.currentTimeMillis()
        imageConverter.convertToJpeg(imagePath)
        val elapsed = System.currentTimeMillis() - start
        logger.info("Conversion took ${elapsed}ms")
    }
}
```

### Issue: Temp Files Not Cleaned Up

**Error:**
```
Disk space decreases continuously; /tmp folder full
```

**Root Causes:**
- Cleanup routine not called
- Exception prevents cleanup
- Cleanup silently fails

**Solutions:**
1. Check cleanup is called in finally block
2. Add explicit cleanup on shutdown hook
3. Log cleanup success/failure
4. Monitor temp folder size

**Implementation:**
```kotlin
fun processImages(images: List<Image>) {
    try {
        for (image in images) {
            val tempFile = converter.convertToJpeg(image)
            llmQueue.enqueue(tempFile)
            // Process...
        }
    } finally {
        // ALWAYS cleanup
        tempManager.cleanup()
        logger.info("Temp files cleaned up")
    }
}
```

---

## LLM Integration Issues

### Issue: Cannot Connect to LMStudio

**Error:**
```
Connection refused: http://localhost:1234/api/v1
```

**Root Causes:**
- LMStudio not running
- Wrong port (default 1234)
- Firewall blocking localhost
- Network interface issue

**Solutions:**
1. **Verify LMStudio running:** `curl http://localhost:1234/api/v1/models`
2. **Check port:** LMStudio settings → port (default 1234)
3. **Test connectivity:** `ping localhost`
4. **Check firewall:** macOS System Preferences → Security & Privacy

**Debug:**
```bash
# Test LMStudio connection
curl http://localhost:1234/api/v1/models

# Check if port is listening
lsof -i :1234

# Start LMStudio (if not running)
# See LMStudio documentation
```

### Issue: LMStudio Returns 429 (Rate Limited)

**Error:**
```
{"error": {"code": 429, "message": "Rate limit exceeded"}}
```

**Root Causes:**
- Sending requests too fast
- LMStudio configured with low rate limit
- Previous requests not completed

**Solutions:**
1. **Verify sequential queue:** Check only one request in-flight
2. **Add delay between requests:** `delay(1000)` between LLM calls
3. **Increase LMStudio rate limit:** LMStudio settings
4. **Reduce concurrent threads:** Lower maxParallelTasks

**Implementation:**
```kotlin
class LLMRequestQueue {
    suspend fun enqueue(request: LLMRequest): Result<LLMResponse> {
        delay(1000) // 1 second delay between requests
        return httpClient.post(request)
    }
}
```

### Issue: LMStudio Model Not Found

**Error:**
```
{"error": {"code": 404, "message": "Model not found: llama2"}}
```

**Root Causes:**
- Model name typo
- Model not loaded in LMStudio
- Model requires download

**Solutions:**
1. Check model name in config (case-sensitive)
2. Load model in LMStudio UI (Models → Select model)
3. Download model if needed (LMStudio → download model)
4. Check available models: `curl http://localhost:1234/api/v1/models`

### Issue: LMStudio Image Analysis Returns Generic Response

**Error:**
```
LLM response: "I cannot see the image clearly."
```

**Root Causes:**
- Image Base64 encoding incorrect
- Image size too large or too small
- Model doesn't support multimodal
- Prompt unclear or wrong format

**Solutions:**
1. **Verify image encoding:** Decode Base64 and save to file; visually inspect
2. **Check image size:** Should be <30 MB; typical 1-5 MB
3. **Verify model supports multimodal:** Check LMStudio compatibility
4. **Test prompt:** Try simpler prompt with LMStudio UI directly
5. **Add image details to prompt:** "Analyze this wallpaper image..."

**Debug:**
```kotlin
fun debugImageEncoding(imagePath: String) {
    val encoded = ImageEncoder.encodeToBase64(imagePath)
    val decoded = Base64.getDecoder().decode(encoded)
    File("/tmp/debug_decoded.jpg").writeBytes(decoded)
    logger.debug("Decoded image saved to /tmp/debug_decoded.jpg")
    logger.debug("Encoded length: ${encoded.length}, decoded length: ${decoded.size}")
}
```

---

## File Operation Issues

### Issue: Atomic Copy Fails, Leaves Partial File

**Error:**
```
Output folder contains corrupted/partial image files
```

**Root Causes:**
- Move/rename operation failed partway
- Temp file deleted before move
- Destination already exists

**Solutions:**
1. Use atomic rename (move temp to destination)
2. Check source exists before copy
3. Verify destination writable before copy
4. Don't delete temp until move succeeds

**Implementation:**
```kotlin
fun atomicCopy(source: File, dest: File): Result<Unit> {
    val temp = File(dest.parentFile, "${dest.name}.tmp")
    return try {
        source.copyTo(temp, overwrite = false)
        if (!temp.renameTo(dest)) {
            Failure(FileIOException("Atomic rename failed"))
        } else {
            Success(Unit)
        }
    } catch (e: Exception) {
        temp.delete() // Cleanup temp
        Failure(FileIOException("Copy failed: ${e.message}"))
    }
}
```

### Issue: Permission Denied on Output Folder

**Error:**
```
java.nio.file.AccessDeniedException: /output/folder
```

**Root Causes:**
- Output folder not writable
- User doesn't own folder
- Folder mounted read-only

**Solutions:**
1. Check permissions: `ls -la /output/folder`
2. Make writable: `chmod u+w /output/folder`
3. Change owner: `chown $USER /output/folder`
4. Check if mounted read-only: `mount | grep output`

**Validation in Config:**
```kotlin
class ConfigValidator {
    fun validateFolders(config: Config): Result<Unit> {
        val output = File(config.folders.output)
        if (!output.canWrite()) {
            return Failure(ConfigurationException("Output folder not writable: ${output.path}"))
        }
        // ... etc
        return Success(Unit)
    }
}
```

---

## Performance Issues

### Issue: Memory Usage Exceeds 2 GB (Heap Out of Memory)

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Root Causes:**
- Batch size too large (>100 images)
- Image files very large (>50 MB each)
- Memory not released between batches
- Thread pool too large (>8 threads)

**Solutions:**
1. **Reduce batch size:** `"processing": {"batchSize": 50}`
2. **Monitor heap:** Use JVM flags `-Xmx2g` (cap memory)
3. **Check image sizes:** Log image dimensions before processing
4. **Force GC:** Add periodic `System.gc()` after batches
5. **Profile memory:** Use visualvm or JProfiler

**Debug Code:**
```kotlin
fun logMemoryUsage(phase: String) {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val max = runtime.maxMemory() / 1024 / 1024
    logger.info("$phase: Memory ${used}MB / ${max}MB")
}
```

### Issue: Processing 1000 Images Takes Hours

**Error:**
```
100 images processed; ETA: 20+ hours
```

**Root Causes:**
- LLM response time very slow (network, model, hardware)
- Sequential LLM requests (unavoidable, by design)
- Image processing slow
- Insufficient parallelization

**Expected Behavior:**
- LLM requests: ~2-5 seconds per image
- 1000 images: 30-80 minutes (LLM-limited)
- Non-LLM: ~1-2 minutes

**Explanation:**
```
Processing timeline (1000 images):
  Sample analysis: ~100 images × 3s per image = 5 minutes (LLM sequential)
  Profile generation: <1 minute
  Candidate evaluation: ~900 images × 3s per image = 45 minutes (LLM sequential)
  Curation: ~2 minutes
  Total: ~52 minutes (LLM-bound)
```

**Optimization:**
1. Reduce LLM response time (faster model, better hardware)
2. Batch LLM requests (if LMStudio supports)
3. Skip LLM for obvious rejects (heuristics)

---

## Testing Issues

### Issue: Tests Fail on Specific Format

**Error:**
```
ImageLoaderTest::testLoadWebP FAILED
AssertionError: Failed to load WebP
```

**Root Causes:**
- WebP codec not available
- Test image corrupted
- Format not supported in test environment

**Solutions:**
1. Skip test for unsupported format: `@Ignore("WebP not supported")`
2. Use fallback test image
3. Install codec: `brew install webp` (if available)

### Issue: LLM Tests Fail (Mock Not Working)

**Error:**
```
MockLLMService.enqueue() returns wrong response
```

**Root Causes:**
- Mock returns unexpected format
- Response parser rejects mock response
- Mock state not reset between tests

**Solutions:**
1. Verify mock response matches expected format
2. Add test setup to reset mock state
3. Log actual vs expected response

**Implementation:**
```kotlin
class LLMIntegrationTest {
    private val mockLLM = MockLLMService()
    
    @Before
    fun setUp() {
        mockLLM.reset() // Clear state
    }
    
    @Test
    fun shouldHandleAnalysisResponse() {
        mockLLM.setupResponse("""
            {"colorPalette": ["blue"], "style": "minimalist", ...}
        """)
        
        val result = runBlocking {
            responseParser.parseAnalysisResponse(mockLLM.getLastResponse())
        }
        
        assertTrue(result is Result.Success)
    }
}
```

---

## Debugging Techniques

### Enable Debug Logging

```kotlin
// In Logger
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

var logLevel = LogLevel.INFO // Change to DEBUG

fun debug(message: String) {
    if (logLevel >= LogLevel.DEBUG) {
        println("[DEBUG] $message")
    }
}
```

### Add Timing Measurements

```kotlin
inline fun <T> timed(name: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val elapsed = System.currentTimeMillis() - start
    logger.info("$name took ${elapsed}ms")
    return result
}

// Usage
timed("Load images") {
    imageLoader.discoverImages(folder)
}
```

### Inspect Temp Files

```bash
# List temp files
ls -lah /path/to/temp/folder

# Check temp file size
du -sh /path/to/temp/folder

# Monitor temp folder growth
watch -n 1 'du -sh /path/to/temp/folder'
```

### Debug LLM Requests

```kotlin
// Log request and response
class DebugLLMClient {
    suspend fun post(request: LLMRequest): Result<LLMResponse> {
        logger.debug("LLM Request: $request")
        val response = httpClient.post(request)
        logger.debug("LLM Response: $response")
        return response
    }
}
```

---

## Escalation Path

If issue cannot be resolved:

1. **Check TROUBLESHOOTING.md** (this file)
2. **Review relevant PHASE documentation** (e.g., PHASE-3-LLM-INTEGRATION.md for LLM issues)
3. **Check ARCHITECTURE-DETAILS.md** for design decisions
4. **Review specs:** `/docs/specs/0-initial-specs/`
5. **Escalate to team lead** with:
   - Error message (full stack trace)
   - Steps to reproduce
   - Attempted solutions
   - Current status

---

**Last Updated:** 2026-03-08
