package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FakeLLMHttpClient : LLMHttpClient {
    var lastRequest: LLMRequest? = null
    var responseToReturn: Result<String> = Result.Success("""
        {
          "choices": [
            {
              "message": {
                "content": [
                  { 
                    "type": "text", 
                    "text": "{ \"colorPalette\": [], \"style\": \"test\", \"mood\": \"test\", \"composition\": \"test\", \"subject\": \"test\", \"technicalNotes\": \"test\", \"quality\": 0.5 }"
                  }
                ]
              }
            }
          ]
        }
    """.trimIndent())
    var delayMs: Long = 0

    override suspend fun send(request: LLMRequest): Result<String> {
        lastRequest = request
        if (delayMs > 0) delay(delayMs)
        return responseToReturn
    }
}

class LLMInternalsSpec : FunSpec({
    val logger = Logger()

    test("LLMRequestQueue processes requests sequentially") {
        val httpClient = FakeLLMHttpClient()
        httpClient.delayMs = 50
        val queue = LLMRequestQueue(httpClient, logger)
        
        val job = launch { queue.run() }
        
        val startTime = System.currentTimeMillis()
        val r1 = async { queue.enqueue(LLMRequest(emptyList())) }
        val r2 = async { queue.enqueue(LLMRequest(emptyList())) }
        
        r1.await()
        r2.await()
        val duration = System.currentTimeMillis() - startTime
        
        (duration >= 100) shouldBe true // At least 2 * 50ms
        
        queue.close()
        job.cancel()
    }

    test("DefaultLLMService encodes images correctly") {
        val httpClient = FakeLLMHttpClient()
        val queue = LLMRequestQueue(httpClient, logger)
        val service = DefaultLLMService(queue, PromptTemplates, LLMResponseParser(), logger)
        
        val job = launch { queue.run() }
        
        val tempFile = Files.createTempFile("test-image", ".jpg").toFile()
        tempFile.writeBytes(byteArrayOf(1, 2, 3))
        
        val image = Image.create(tempFile.absolutePath, ImageFormat.JPEG, 100, 100, 3)
        
        service.analyzeSampleImage(image)
        
        httpClient.lastRequest shouldNotBe null
        val imageDataPart = httpClient.lastRequest?.messages?.first()?.contentParts?.find { it is ChatContentPart.ImageDataUrl } as ChatContentPart.ImageDataUrl
        imageDataPart.dataUrl shouldStartWith "data:image/jpeg;base64,"
        
        tempFile.delete()
        job.cancel()
    }

    test("DefaultLLMService fails on missing image file") {
        val httpClient = FakeLLMHttpClient()
        val queue = LLMRequestQueue(httpClient, logger)
        val service = DefaultLLMService(queue, PromptTemplates, LLMResponseParser(), logger)
        
        val image = Image.create("/nonexistent.jpg", ImageFormat.JPEG, 100, 100, 0)
        val result = service.analyzeSampleImage(image)
        
        result.shouldBeInstanceOf<Result.Failure>()
        result.error.message shouldBe "Image file for LLM does not exist: /nonexistent.jpg"
    }

    test("DefaultLLMService fails on too large image file") {
        val httpClient = FakeLLMHttpClient()
        val queue = LLMRequestQueue(httpClient, logger)
        val service = DefaultLLMService(queue, PromptTemplates, LLMResponseParser(), logger)
        
        val tempFile = Files.createTempFile("large-image", ".jpg").toFile()
        // Create 11MB file
        val largeData = ByteArray(11 * 1024 * 1024)
        tempFile.writeBytes(largeData)
        
        val image = Image.create(tempFile.absolutePath, ImageFormat.JPEG, 100, 100, tempFile.length())
        val result = service.analyzeSampleImage(image)
        
        result.shouldBeInstanceOf<Result.Failure>()
        result.error.message shouldStartWith "Image file for LLM is too large"
        
        tempFile.delete()
    }
})
