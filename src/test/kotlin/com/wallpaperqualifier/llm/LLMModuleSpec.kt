package com.wallpaperqualifier.llm

import com.wallpaperqualifier.config.AppConfig
import com.wallpaperqualifier.config.FoldersConfig
import com.wallpaperqualifier.config.LLMConfig
import com.wallpaperqualifier.config.ProcessingConfig
import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.ResolutionRange
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LLMModuleSpec : FunSpec({

    test("createDefaultLLMService wires config into HTTP client and queue") {
        val config = AppConfig(
            folders = FoldersConfig(
                samples = "/tmp/samples",
                candidates = "/tmp/candidates",
                output = "/tmp/output",
                temp = "/tmp/temp"
            ),
            llm = LLMConfig(
                endpoint = "http://localhost:1234/api/v1",
                model = "test-model",
                apiKey = "secret"
            ),
            processing = ProcessingConfig()
        )

        val logger = Logger()

        val service = createDefaultLLMService(config, logger)

        service.shouldBeInstanceOf<LLMService>()
    }

    test("LLMResponseParser fails on invalid analysis JSON envelope") {
        val parser = LLMResponseParser()
        // Envelope that does not contain valid inner JSON for our schema
        val result = parser.parseAnalysisResponse(
            """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      { "type": "text", "text": "not json at all" }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        result.shouldBeInstanceOf<Result.Failure<*>>()
    }

    test("LLMResponseParser parses valid analysis response from OpenAI-style envelope") {
        val parser = LLMResponseParser()
        val envelope =
            """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      { 
                        "type": "text", 
                        "text": "{ \"colorPalette\": [\"#112233\", \"#445566\"], \"style\": \"minimalist\", \"mood\": \"calming\", \"composition\": \"rule_of_thirds\", \"subject\": \"nature\", \"technicalNotes\": \"Sharp and well lit\", \"quality\": 0.9 }"
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()

        val result = parser.parseAnalysisResponse(envelope)

        result.shouldBeInstanceOf<Result.Success<ImageCharacteristics>>()
        val characteristics = (result as Result.Success).value
        characteristics.style shouldBe "minimalist"
        characteristics.quality shouldBe 0.9f
    }

    test("LLMResponseParser parses valid evaluation response from OpenAI-style envelope") {
        val parser = LLMResponseParser()
        val envelope =
            """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      { 
                        "type": "text", 
                        "text": "{ \"qualified\": true, \"confidenceScore\": 0.85, \"reasoning\": \"Matches color palette and mood preferences.\" }"
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()

        val result = parser.parseEvaluationResponse(envelope, "/tmp/image.jpg")

        result.shouldBeInstanceOf<Result.Success<EvaluationResult>>()
        val evaluation = (result as Result.Success).value
        evaluation.qualified shouldBe true
        evaluation.confidenceScore shouldBe 0.85f
    }
})
