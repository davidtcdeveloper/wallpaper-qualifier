package com.wallpaperqualifier.domain

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class ModelsTest {

    @Test
    fun testImageCreation() {
        val image = Image.create(
            path = "/path/to/image.jpg",
            format = ImageFormat.JPEG,
            width = 1920,
            height = 1080,
            fileSize = 512000L
        )

        assertEquals("/path/to/image.jpg", image.path)
        assertEquals(ImageFormat.JPEG, image.format)
        assertEquals(1920, image.width)
        assertEquals(1080, image.height)
        assertEquals(512000L, image.fileSize)
        assertTrue(image.aspectRatio > 1.77f && image.aspectRatio < 1.78f)
    }

    @Test
    fun testImageCharacteristics() {
        val characteristics = ImageCharacteristics(
            colorPalette = listOf("blue", "green", "yellow"),
            style = "modern",
            mood = "calm",
            composition = "landscape",
            subject = "nature",
            technicalNotes = "High quality, well-lit",
            quality = 0.95f
        )

        assertEquals(3, characteristics.colorPalette.size)
        assertEquals("modern", characteristics.style)
        assertEquals(0.95f, characteristics.quality)
    }

    @Test
    fun testQualityProfile() {
        val profile = QualityProfile(
            preferredColorPalettes = listOf("blue", "green"),
            preferredStyles = listOf("modern", "minimalist"),
            preferredMoods = listOf("calm", "peaceful"),
            preferredCompositions = listOf("landscape"),
            commonSubjects = listOf("nature", "architecture"),
            resolutionPreferences = ResolutionRange(),
            averageQuality = 0.92f,
            sampleCount = 5
        )

        assertEquals(5, profile.sampleCount)
        assertEquals(2, profile.preferredColorPalettes.size)
        assertEquals(0.92f, profile.averageQuality)
    }

    @Test
    fun testEvaluationResult() {
        val result = EvaluationResult(
            imagePath = "/path/to/candidate.jpg",
            qualified = true,
            confidenceScore = 0.87f,
            reasoning = "Matches color palette and style preferences"
        )

        assertTrue(result.qualified)
        assertEquals(0.87f, result.confidenceScore)
        assertEquals("/path/to/candidate.jpg", result.imagePath)
    }

    @Test
    fun testResultSuccess() {
        val result: Result<String> = Result.Success("test value")

        assertEquals("test value", result.getOrNull())
        assertEquals("test value", result.getOrThrow())
    }

    @Test
    fun testResultFailure() {
        val exception = Exception("Test error")
        val result: Result<String> = Result.Failure(exception)

        assertNull(result.getOrNull())
        try {
            result.getOrThrow()
            assertFalse(true) // Should not reach here
        } catch (e: Exception) {
            assertEquals("Test error", e.message)
        }
    }

    @Test
    fun testResultMap() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }

        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun testCustomExceptions() {
        try {
            throw ConfigurationException("Invalid config")
        } catch (e: ConfigurationException) {
            assertEquals("Invalid config", e.message)
        }

        try {
            throw ImageProcessingException("Processing failed")
        } catch (e: ImageProcessingException) {
            assertEquals("Processing failed", e.message)
        }

        try {
            throw LLMException("LLM error")
        } catch (e: LLMException) {
            assertEquals("LLM error", e.message)
        }

        try {
            throw FileIOException("File not found")
        } catch (e: FileIOException) {
            assertEquals("File not found", e.message)
        }
    }
}
