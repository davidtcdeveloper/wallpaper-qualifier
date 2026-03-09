package com.wallpaperqualifier.config

import com.wallpaperqualifier.domain.ConfigurationException
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigParserTest {

    private val parser = ConfigParser()

    private val validConfigJson = """
        {
            "folders": {
                "samples": "/tmp",
                "candidates": "/tmp",
                "output": "/tmp",
                "temp": "/tmp"
            },
            "llm": {
                "endpoint": "http://localhost:1234/api/v1",
                "model": "llama2"
            },
            "processing": {
                "maxParallelTasks": 8,
                "outputFormat": "original",
                "jpegQuality": 90
            }
        }
    """.trimIndent()

    @Test
    fun testParseValidJson() {
        val result = parser.parseJson(validConfigJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        val config = (result as com.wallpaperqualifier.domain.Result.Success).value
        
        assertEquals("/tmp", config.folders.samples)
        assertEquals("/tmp", config.folders.candidates)
        assertEquals("/tmp", config.folders.output)
        assertEquals("/tmp", config.folders.temp)
        assertEquals("http://localhost:1234/api/v1", config.llm.endpoint)
        assertEquals("llama2", config.llm.model)
        assertEquals(8, config.processing.maxParallelTasks)
        assertEquals("original", config.processing.outputFormat)
        assertEquals(90, config.processing.jpegQuality)
    }

    @Test
    fun testParseWithDefaults() {
        val minimalConfigJson = """
            {
                "folders": {
                    "samples": "/tmp",
                    "candidates": "/tmp",
                    "output": "/tmp",
                    "temp": "/tmp"
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(minimalConfigJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        val config = (result as com.wallpaperqualifier.domain.Result.Success).value
        
        // Should use defaults
        assertEquals("http://localhost:1234/api/v1", config.llm.endpoint)
        assertEquals("llama2", config.llm.model)
        assertEquals(8, config.processing.maxParallelTasks)
        assertEquals("original", config.processing.outputFormat)
        assertEquals(90, config.processing.jpegQuality)
    }

    @Test
    fun testParseInvalidJson() {
        val invalidJson = "{ invalid json }"
        
        val result = parser.parseJson(invalidJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error is ConfigurationException)
    }

    @Test
    fun testMissingRequiredField() {
        val missingFoldersJson = """
            {
                "llm": {
                    "endpoint": "http://localhost:1234/api/v1",
                    "model": "llama2"
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(missingFoldersJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
    }

    @Test
    fun testValidateEmptySamplesFolder() {
        val emptyFoldersJson = """
            {
                "folders": {
                    "samples": "",
                    "candidates": "/tmp",
                    "output": "/tmp",
                    "temp": "/tmp"
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(emptyFoldersJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("samples path cannot be empty") == true)
    }

    @Test
    fun testValidateInvalidMaxParallelTasks() {
        val invalidParallelJson = """
            {
                "folders": {
                    "samples": "/tmp",
                    "candidates": "/tmp",
                    "output": "/tmp",
                    "temp": "/tmp"
                },
                "processing": {
                    "maxParallelTasks": 256
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(invalidParallelJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("maxParallelTasks must be between 1 and 128") == true)
    }

    @Test
    fun testValidateInvalidJpegQuality() {
        val invalidQualityJson = """
            {
                "folders": {
                    "samples": "/tmp",
                    "candidates": "/tmp",
                    "output": "/tmp",
                    "temp": "/tmp"
                },
                "processing": {
                    "jpegQuality": 150
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(invalidQualityJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("jpegQuality must be between 1 and 100") == true)
    }

    @Test
    fun testValidateInvalidOutputFormat() {
        val invalidFormatJson = """
            {
                "folders": {
                    "samples": "/tmp",
                    "candidates": "/tmp",
                    "output": "/tmp",
                    "temp": "/tmp"
                },
                "processing": {
                    "outputFormat": "gif"
                }
            }
        """.trimIndent()
        
        val result = parser.parseJson(invalidFormatJson)
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("outputFormat must be") == true)
    }

    @Test
    fun testParseFile() {
        // Create a temporary config file
        val tempFile = Files.createTempFile("test-config", ".json").toFile()
        tempFile.writeText(validConfigJson)
        
        try {
            val result = parser.parseFile(tempFile.absolutePath)
            
            assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
            val config = (result as com.wallpaperqualifier.domain.Result.Success).value
            assertEquals("/tmp", config.folders.samples)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testParseFileNotFound() {
        val result = parser.parseFile("/nonexistent/config.json")
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("not found") == true)
    }

    @Test
    fun testParseFileIsDirectory() {
        val tempDir = Files.createTempDirectory("test-config-dir").toFile()
        
        try {
            val result = parser.parseFile(tempDir.absolutePath)
            
            assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
            val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
            assertTrue(error.message?.contains("not a file") == true)
        } finally {
            tempDir.delete()
        }
    }
}
