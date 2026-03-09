package com.wallpaperqualifier.config

import com.wallpaperqualifier.domain.ConfigurationException
import com.wallpaperqualifier.domain.Result
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

class ConfigParserTest : FunSpec({

    val validConfigJson = """
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

    test("valid json parses into configuration") {
        val result = ConfigParser().parseJson(validConfigJson)

        val config = result.shouldBeInstanceOf<Result.Success<AppConfig>>().value
        config.folders.samples shouldBe "/tmp"
        config.folders.candidates shouldBe "/tmp"
        config.folders.output shouldBe "/tmp"
        config.folders.temp shouldBe "/tmp"
        config.llm.endpoint shouldBe "http://localhost:1234/api/v1"
        config.llm.model shouldBe "llama2"
        config.processing.maxParallelTasks shouldBe 8
        config.processing.outputFormat shouldBe "original"
        config.processing.jpegQuality shouldBe 90
    }

    test("missing optional sections fall back to defaults") {
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

        val result = ConfigParser().parseJson(minimalConfigJson)

        val config = result.shouldBeInstanceOf<Result.Success<AppConfig>>().value
        config.llm.endpoint shouldBe "http://localhost:1234/api/v1"
        config.llm.model shouldBe "llama2"
        config.processing.maxParallelTasks shouldBe 8
        config.processing.outputFormat shouldBe "original"
        config.processing.jpegQuality shouldBe 90
    }

    test("invalid json returns configuration exception") {
        val result = ConfigParser().parseJson("{ invalid json }")

        val failure = result.shouldBeInstanceOf<Result.Failure<*>>()
        failure.error.shouldBeInstanceOf<ConfigurationException>()
    }

    test("missing required folders fails validation") {
        val missingFoldersJson = """
            {
                "llm": {
                    "endpoint": "http://localhost:1234/api/v1",
                    "model": "llama2"
                }
            }
        """.trimIndent()

        ConfigParser().parseJson(missingFoldersJson).failureMessageShouldContain("folders")
    }

    test("empty samples folder fails validation") {
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

        ConfigParser().parseJson(emptyFoldersJson).failureMessageShouldContain("samples path cannot be empty")
    }

    test("invalid parallel tasks value fails validation") {
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

        ConfigParser().parseJson(invalidParallelJson).failureMessageShouldContain("maxParallelTasks must be between 1 and 128")
    }

    test("invalid jpeg quality fails validation") {
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

        ConfigParser().parseJson(invalidQualityJson).failureMessageShouldContain("jpegQuality must be between 1 and 100")
    }

    test("invalid output format fails validation") {
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

        ConfigParser().parseJson(invalidFormatJson).failureMessageShouldContain("outputFormat must be")
    }

    test("parse file succeeds from disk") {
        withTempConfigFile(validConfigJson) { tempFile ->
            val result = ConfigParser().parseFile(tempFile.absolutePath)

            val config = result.shouldBeInstanceOf<Result.Success<AppConfig>>().value
            config.folders.samples shouldBe "/tmp"
        }
    }

    test("parse file not found returns failure") {
        ConfigParser().parseFile("/nonexistent/config.json").failureMessageShouldContain("not found")
    }

    test("parse file pointing at directory returns failure") {
        val tempDir = Files.createTempDirectory("test-config-dir").toFile()
        try {
            ConfigParser().parseFile(tempDir.absolutePath).failureMessageShouldContain("not a file")
        } finally {
            tempDir.delete()
        }
    }
})

private fun Result<*>.failureMessageShouldContain(expected: String) {
    val failure = this.shouldBeInstanceOf<Result.Failure<*>>()
    failure.error.message.shouldNotBeNull().shouldContain(expected)
}

private fun withTempConfigFile(content: String, block: (File) -> Unit) {
    val tempFile = Files.createTempFile("test-config", ".json").toFile()
    try {
        tempFile.writeText(content)
        block(tempFile)
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
