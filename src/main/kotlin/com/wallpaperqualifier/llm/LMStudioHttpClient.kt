package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.UnknownHostException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a single logical LLM request payload to LMStudio.
 *
 * At this layer we work directly with the OpenAI-compatible chat request
 * format used by LMStudio.
 */
data class LLMRequest(
    val messages: List<ChatMessage>,
    val maxTokens: Int = 500,
    val temperature: Double = 0.7
)

/**
 * Minimal representation of a chat message compatible with OpenAI format.
 */
data class ChatMessage(
    val role: String,
    val contentParts: List<ChatContentPart>
)

sealed class ChatContentPart {
    data class Text(val text: String) : ChatContentPart()
    data class ImageDataUrl(val dataUrl: String) : ChatContentPart()
}

/**
 * Ktor-based HTTP client for LMStudio's OpenAI-compatible chat completions API.
 */
class LMStudioHttpClient(
    private val endpoint: String,
    private val model: String,
    private val apiKey: String?,
    private val logger: Logger,
    private val json: Json = Json { ignoreUnknownKeys = false; isLenient = false }
) : LLMHttpClient {

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@LMStudioHttpClient.json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            if (!apiKey.isNullOrBlank()) {
                headers.append(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }
    }

    /**
     * Sends a single request to LMStudio and returns the raw JSON response body
     * or a domain-level error wrapped in Result.
     */
    override suspend fun send(request: LLMRequest): Result<String> {
        return try {
            val payload = buildPayload(request)

            val responseText = withTimeout(30.seconds) {
                val httpResponse: HttpResponse = httpClient.post(endpoint.trimEnd('/') + "/chat/completions") {
                    setBody(payload)
                }
                when (httpResponse.status) {
                    HttpStatusCode.OK -> httpResponse.bodyAsText()
                    HttpStatusCode.Unauthorized,
                    HttpStatusCode.Forbidden,
                    HttpStatusCode.BadRequest,
                    HttpStatusCode.UnprocessableEntity -> {
                        throw LLMError.Api(
                            "LMStudio API error ${httpResponse.status.value}: ${httpResponse.bodyAsText()}"
                        )
                    }
                    HttpStatusCode.TooManyRequests,
                    HttpStatusCode.InternalServerError,
                    HttpStatusCode.BadGateway,
                    HttpStatusCode.ServiceUnavailable,
                    HttpStatusCode.GatewayTimeout -> {
                        throw LLMError.Api(
                            "LMStudio temporary error ${httpResponse.status.value}: ${httpResponse.bodyAsText()}"
                        )
                    }
                    else -> {
                        throw LLMError.Api(
                            "LMStudio unexpected status ${httpResponse.status.value}: ${httpResponse.bodyAsText()}"
                        )
                    }
                }
            }

            Result.Success(responseText)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("LLM HTTP request timed out: ${e.message}", e)
            Result.Failure(
                LLMError.Timeout("LLM request timed out: ${e.message}")
            )
        } catch (e: ConnectException) {
            logger.error("LLM connection failed: ${e.message}", e)
            Result.Failure(
                LLMError.Network("Failed to connect to LLM endpoint: ${e.message}", e)
            )
        } catch (e: UnknownHostException) {
            logger.error("LLM host could not be resolved: ${e.message}", e)
            Result.Failure(
                LLMError.Network("Unknown LLM host: ${e.message}", e)
            )
        } catch (e: LLMError) {
            // Already mapped to a domain-specific LLM error above.
            Result.Failure(e)
        } catch (e: Exception) {
            logger.error("LLM HTTP request failed: ${e.message}", e)
            Result.Failure(
                LLMError.Network("Failed to send request to LLM: ${e.message}", e)
            )
        }
    }

    private fun buildPayload(request: LLMRequest): JsonObject {
        return buildJsonObject {
            put("model", model)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            putJsonArray("messages") {
                request.messages.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", message.role)
                            putJsonArray("content") {
                                message.contentParts.forEach { part ->
                                    when (part) {
                                        is ChatContentPart.Text -> add(
                                            buildJsonObject {
                                                put("type", "text")
                                                put("text", part.text)
                                            }
                                        )
                                        is ChatContentPart.ImageDataUrl -> add(
                                            buildJsonObject {
                                                put("type", "image_url")
                                                putJsonObject("image_url") {
                                                    put("url", part.dataUrl)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Internal interface to decouple the HTTP transport from the request queue
 * and allow simple fakes in tests.
 */
interface LLMHttpClient {
    suspend fun send(request: LLMRequest): Result<String>
}

