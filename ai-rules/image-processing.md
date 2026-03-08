<primary_directive>
You handle image processing with PRECISION and CLARITY. ALWAYS verify format support. ALWAYS optimize without quality loss. NEVER assume image validity - validate and fail gracefully.
</primary_directive>

# Image Processing: Format Handling & Optimization

## Overview

The Wallpaper Qualifier must handle diverse image formats and optimize them for storage and LLM processing. This document covers format support, conversion patterns, and quality considerations.

---

## Format Support

<rule_1 priority="HIGHEST">
**SUPPORTED FORMATS**: We support JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, and RAW (CR2, NEF, ARW, DNG).
- All formats must be detected automatically (not by file extension)
- All formats must convert to PNG or JPEG for output
- RAW formats require special handling (larger files, professional metadata)

**MUST**:
- ✓ Detect format from file magic bytes, not extension
- ✓ Provide clear error if format unsupported
- ✓ Support both lossy (JPEG) and lossless (PNG) output
- ✓ Preserve EXIF/metadata when relevant

**Format Detection - Good**:
```kotlin
enum class ImageFormat {
    JPEG, PNG, HEIC, WEBP, TIFF, BMP, GIF, RAW_CR2, RAW_NEF, RAW_ARW, RAW_DNG
}

suspend fun detectFormat(path: String): ImageFormat {
    val bytes = File(path).readBytes().take(32)
    return when {
        bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) -> ImageFormat.JPEG
        bytes.startsWith(byteArrayOf(0x89, 0x50, 0x4E, 0x47)) -> ImageFormat.PNG
        bytes.startsWith(byteArrayOf(0x49, 0x49, 0x2A, 0x00)) -> detectTiffVariant(bytes)
        // ... other formats
        else -> ImageFormat.UNKNOWN
    }
}

// Or use Koog's built-in detection
val format = ImageDecoder.detectFormat(File(path))
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Extension-based detection
fun getFormat(path: String): ImageFormat {
    return when (path.substringAfterLast(".").lowercase()) {
        "jpg", "jpeg" -> ImageFormat.JPEG
        "png" -> ImageFormat.PNG
        // User renames .jpg to .png → breaks
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**CONVERSION PIPELINE**: Convert all formats to PNG (lossless) or JPEG (efficient) for output.
- Default to PNG for quality-conscious workflows
- Offer JPEG as option for file size priority
- Never lose quality during conversion
- Handle color space conversion (sRGB recommended)

**MUST**:
- ✓ Decode input format to raw image data
- ✓ Normalize color space to sRGB
- ✓ Encode to target format with quality settings
- ✓ Validate output is valid before returning

**Example - Good**:
```kotlin
data class ImageConversionConfig(
    val targetFormat: TargetFormat = TargetFormat.PNG,
    val jpegQuality: Int = 85, // Ignored if PNG
    val maxFileSizeBytes: Int = 2_097_152 // 2MB
)

enum class TargetFormat { PNG, JPEG }

suspend fun convertImage(
    sourcePath: String,
    targetPath: String,
    config: ImageConversionConfig
): ConversionResult = try {
    // Decode from any supported format
    val sourceFormat = detectFormat(sourcePath)
    val rawImage = ImageDecoder.decode(sourcePath)
    
    // Normalize color space
    val normalized = rawImage.normalizeToSRGB()
    
    // Encode to target format
    val encoded = when (config.targetFormat) {
        TargetFormat.PNG -> PNGEncoder.encode(normalized)
        TargetFormat.JPEG -> JPEGEncoder.encode(normalized, quality = config.jpegQuality)
    }
    
    // Validate size
    if (encoded.sizeBytes > config.maxFileSizeBytes) {
        return ConversionResult.Failed("Output exceeds max size: ${encoded.sizeBytes} > ${config.maxFileSizeBytes}")
    }
    
    // Write atomically
    val tempFile = File(targetPath + ".tmp")
    tempFile.writeBytes(encoded)
    tempFile.renameTo(File(targetPath))
    
    ConversionResult.Success(targetPath)
} catch (e: Exception) {
    ConversionResult.Failed("Conversion failed: ${e.message}")
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Direct conversion without validation
fun convertImage(sourcePath: String, targetPath: String) {
    val bytes = File(sourcePath).readBytes()
    val converted = ImageLibrary.convert(bytes, "png")
    File(targetPath).writeBytes(converted)
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**OPTIMIZATION STRATEGY**: Reduce file size while maintaining visual quality.
- Start with aggressive settings, back off if quality suffers
- Use adaptive quality based on image characteristics
- Resize if appropriate (wallpapers rarely need >4K)
- Always verify output quality visually before deployment

**MUST**:
- ✓ JPEG: Quality 85 balances quality and size
- ✓ PNG: Use appropriate compression level (typically 6-9)
- ✓ Resize to max width/height if too large (e.g., max 4096px)
- ✓ Remove unnecessary metadata
- ✓ Target under 2MB per image

**Example - Good**:
```kotlin
object ImageOptimization {
    const val DEFAULT_JPEG_QUALITY = 85
    const val PNG_COMPRESSION_LEVEL = 9
    const val MAX_DIMENSION_PIXELS = 4096
    const val MAX_FILE_SIZE_BYTES = 2_097_152
}

suspend fun optimizeImage(image: Image): OptimizedImage {
    var working = image
    
    // 1. Resize if too large
    if (image.width > ImageOptimization.MAX_DIMENSION_PIXELS ||
        image.height > ImageOptimization.MAX_DIMENSION_PIXELS) {
        working = working.resizeToFit(ImageOptimization.MAX_DIMENSION_PIXELS)
    }
    
    // 2. Remove metadata unless needed
    working = working.removeMetadata(keep = listOf("exif.orientation"))
    
    // 3. Encode with quality settings
    val encoded = when (shouldUseLossy(working)) {
        true -> JPEGEncoder.encode(working, quality = 85)
        false -> PNGEncoder.encode(working, compression = 9)
    }
    
    return OptimizedImage(encoded, working.format)
}

private fun shouldUseLossy(image: Image): Boolean {
    // Use JPEG for photographs, PNG for graphics
    return image.hasHighColorVariance()
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Destructive compression
fun optimizeImage(image: Image): OptimizedImage {
    val aggressivelyCompressed = image.compress(quality = 30)
    return aggressivelyCompressed // Visible quality loss
}
```
</rule_3>

---

<rule_4 priority="HIGH">
**ERROR HANDLING**: Corrupted or unsupported images fail gracefully without crashing.
- Skip corrupted files
- Report what went wrong
- Continue processing other images
- Collect results for summary

**MUST**:
- ✓ Catch decode errors specifically
- ✓ Report file path and error reason
- ✓ Never let one bad image stop entire batch
- ✓ Log errors with sufficient context

**Example - Good**:
```kotlin
sealed class ImageProcessResult {
    data class Success(val path: String, val bytes: ByteArray) : ImageProcessResult()
    data class SkippedCorrupted(val path: String, val reason: String) : ImageProcessResult()
    data class SkippedUnsupported(val path: String, val format: String) : ImageProcessResult()
}

suspend fun processBatch(imagePaths: List<String>): List<ImageProcessResult> {
    return imagePaths.map { path ->
        try {
            val decoded = ImageDecoder.decode(path)
            val optimized = optimizeImage(decoded)
            ImageProcessResult.Success(path, optimized.bytes)
        } catch (e: CorruptedImageException) {
            logger.warn("Skipping corrupted image: $path (${e.message})")
            ImageProcessResult.SkippedCorrupted(path, e.message ?: "Unknown")
        } catch (e: UnsupportedFormatException) {
            logger.warn("Skipping unsupported format: $path (${e.detectedFormat})")
            ImageProcessResult.SkippedUnsupported(path, e.detectedFormat)
        } catch (e: Exception) {
            logger.error("Error processing $path", e)
            ImageProcessResult.SkippedCorrupted(path, "Unexpected error: ${e.message}")
        }
    }
}

// Report summary
val results = processBatch(paths)
val successful = results.filterIsInstance<ImageProcessResult.Success>()
val corrupted = results.filterIsInstance<ImageProcessResult.SkippedCorrupted>()
println("Processed: ${successful.size} success, ${corrupted.size} corrupted")
```

**Example - Avoid**:
```kotlin
// Anti-pattern: One corrupted image stops everything
suspend fun processBatch(imagePaths: List<String>): List<ByteArray> {
    return imagePaths.map { path ->
        val decoded = ImageDecoder.decode(path) // Throws on corruption
        val optimized = optimizeImage(decoded)
        optimized.bytes // Batch stops here on error
    }
}
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**RAW FORMAT HANDLING**: RAW formats require special handling and may not be suitable for all workflows.
- RAW files are 10-100x larger than JPEG/PNG
- Require debayering (camera-specific)
- Preserve lossless quality
- Consider user preference before processing

**MUST**:
- ✓ Document RAW format limitations
- ✓ Offer option to skip RAW files for speed
- ✓ Use dedicated RAW library (dcraw, LibRaw via FFI)
- ✓ Warn user if RAW processing will be slow

**Example - Good**:
```kotlin
config.enableRawProcessing = false // User choice, documented

suspend fun loadImage(path: String, config: ProcessingConfig): Image = try {
    val format = detectFormat(path)
    when {
        format.isRaw && !config.enableRawProcessing -> {
            logger.info("Skipping RAW file: $path (raw processing disabled)")
            return Image.Placeholder
        }
        format.isRaw -> {
            logger.info("Processing RAW file: $path (may take longer)")
            processRawImage(path)
        }
        else -> ImageDecoder.decode(path)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Silently slow down processing
fun loadImage(path: String): Image {
    val format = detectFormat(path)
    if (format.isRaw) {
        // Process RAW without warning user - takes 10 minutes!
        return RawDecoder.decode(path)
    }
    return ImageDecoder.decode(path)
}
```
</rule_5>

---

## Checklist

Before processing images:

☐ **Format Detection**: Using magic bytes, not file extension
☐ **Conversion Pipeline**: All formats convert to PNG or JPEG
☐ **Optimization**: File size under 2MB, quality preserved
☐ **Error Handling**: Corrupted images skip gracefully
☐ **Batch Processing**: Parallel processing with result collection
☐ **Metadata**: Removing unnecessary metadata, preserving orientation
☐ **RAW Handling**: User choice documented, warnings in place
☐ **Atomic Writes**: No partial files on disk

---

**Last Updated**: 2026-03-08
