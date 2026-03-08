
# LMStudio Local LLM API Specification

## Table of Contents
1. [Introduction](#introduction)
2. [Wallpaper Qualifier integration decisions](#wallpaper-qualifier-integration-decisions)
3. [Base URL and Authentication](#base-url-and-authentication)
4. [Endpoints](#endpoints)
   - [Chat Completion](#chat-completion)
   - [Image Upload and Multimodal Prompts](#image-upload-and-multimodal-prompts)
5. [Request Format](#request-format)
6. [Response Format](#response-format)
7. [Error Handling](#error-handling)
8. [Examples](#examples)

---

## Introduction
LMStudio provides a RESTful API for interacting with locally loaded Large Language Models (LLMs). This API supports both text and image inputs, enabling multimodal interactions. The API is designed to be OpenAI-compatible, with additional LMStudio-specific features.

---

## Wallpaper Qualifier integration decisions

For the Wallpaper Qualifier CLI, the following approaches are considered. The **Selected approach** column records the decision for this project.

| **Area**                | **Suggested approaches**                                                                 | **Selected approach** |
| ----------------------- | ---------------------------------------------------------------------------------------- | --------------------- |
| **Platform**            | macOS 13+, macOS 26+                                                                     | **macOS 26+**         |
| **Framework**           | Kotlin Koog, Skiko, native macOS APIs, JVM libraries                                      | **Kotlin Koog**       |
| **Image processing**    | Kotlin Koog, Skiko, native APIs, Apache Commons Imaging, TwelveMonkeys                    | **Kotlin Koog**       |
| **LLM provider**        | OpenAI GPT-4 Vision, Anthropic Claude 3, Google Gemini Vision, any provider supported by Koog | **User choice (any supported by Koog)**; no specific recommendation |
| **Retry policy**        | 3 retries with exponential backoff, no retries                                           | **No retry strategy** |
| **Performance targets** | <5s per image, 100 images in <10 minutes; none for first version                         | **None for first version** |
| **Temp folder isolation** | Isolated temp folder with immediate cleanup                                              | **Isolated temp folder; delete files immediately after use** |

---

## Base URL and Authentication
- **Base URL:** `http://localhost:1234/api/v1`
- **Authentication:** Optional. If enabled, include a bearer token in the `Authorization` header:
  ```http
  Authorization: Bearer $LM_API_TOKEN
  ```

---

## Endpoints

### Chat Completion
**Endpoint:** `/chat/completions`
**Method:** `POST`
**Description:** Run a chat completion with the loaded LLM.

#### Request Body
```json
{
  "model": "string",          // Optional: Specify the model ID if multiple are loaded
  "messages": [               // Array of message objects
    {
      "role": "system|user|assistant",
      "content": "string"     // Text content or image reference
    }
  ],
  "max_tokens": int,          // Optional: Maximum number of tokens to generate
  "temperature": float,       // Optional: Sampling temperature (0.0 to 2.0)
  "stream": boolean,          // Optional: Enable streaming responses
  "gpu_offload": boolean,     // Optional: Offload computation to GPU
  "ttl": int                  // Optional: Time-to-live for the session in seconds
}
```

#### Image References in Prompts
Images can be referenced in the `content` field using one of the following formats:
- **Base64:** `data:image/png;base64,<base64_string>`
- **File Path:** `file:///path/to/image.png`
- **URL:** `http://localhost:1234/api/v1/files/<file_id>`

---

### Image Upload and Multimodal Prompts
**Endpoint:** `/files`
**Method:** `POST`
**Description:** Upload an image to be used in a prompt.

#### Request Body
```json
{
  "file": "base64_string",    // Base64-encoded image data
  "filename": "string"        // Optional: Filename for the image
}
```

#### Response
```json
{
  "id": "string",             // Unique file ID for referencing the image
  "url": "string"             // URL to access the uploaded image
}
```

---

## Request Format
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer $LM_API_TOKEN` (if authentication is enabled)

- **Body:** JSON object as described in the [Endpoints](#endpoints) section.

---

## Response Format
- **Success:**
  ```json
  {
    "id": "string",           // Unique response ID
    "choices": [
      {
        "index": int,
        "message": {
          "role": "assistant",
          "content": "string" // Generated text or image reference
        },
        "finish_reason": "stop|length|error"
      }
    ],
    "usage": {
      "prompt_tokens": int,
      "completion_tokens": int,
      "total_tokens": int
    }
  }
  ```

- **Streaming:**
  ```http
  HTTP/1.1 200 OK
  Content-Type: text/event-stream

  data: {"id":"string","choices":[{"delta":{"content":"streamed"}}]}
  ```

---

## Error Handling
- **Error Response:**
  ```json
  {
    "error": {
      "message": "string",    // Error description
      "type": "string",       // Error type (e.g., "invalid_request_error")
      "code": "string"        // Error code (e.g., "400")
    }
  }
  ```

- **Common Errors:**
  - `400 Bad Request`: Invalid input format.
  - `401 Unauthorized`: Missing or invalid API token.
  - `404 Not Found`: Endpoint or resource not found.
  - `429 Too Many Requests`: Rate limit exceeded.
  - `500 Internal Server Error`: Server-side issue.

---

## Examples

### Example 1: Chat Completion with Text
```bash
curl -X POST http://localhost:1234/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LM_API_TOKEN" \
  -d '{
    "messages": [
      {"role": "user", "content": "Hello, how are you?"}
    ]
  }'
```

### Example 2: Chat Completion with Image
```bash
curl -X POST http://localhost:1234/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LM_API_TOKEN" \
  -d '{
    "messages": [
      {"role": "user", "content": [
        {"type": "text", "text": "Describe this image"},
        {"type": "image_url", "image_url": {"url": "file:///path/to/image.png"}}
      ]}
    ]
  }'
```

### Example 3: Upload Image
```bash
curl -X POST http://localhost:1234/api/v1/files \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LM_API_TOKEN" \
  -d '{
    "file": "base64_encoded_image_data",
    "filename": "image.png"
  }'
```

---

## Notes
- **Image Processing:** Uploaded images are processed in-memory with a default size limit of 30 MB and resized to 2048px.
- **Rate Limiting:** The API enforces rate limits to prevent abuse. Adjust limits in LMStudio settings if needed.
- **Compatibility:** The API is designed to be OpenAI-compatible, but LMStudio-specific features (e.g., GPU offloading) are available.
