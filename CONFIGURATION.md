# Configuration Guide

Wallpaper Qualifier uses a JSON configuration file to define folders, LLM settings, and processing parameters.

## JSON Schema

```json
{
  "folders": {
    "samples": "string",
    "candidates": "string",
    "output": "string",
    "temp": "string"
  },
  "llm": {
    "endpoint": "string",
    "model": "string",
    "apiKey": "string (optional)"
  },
  "processing": {
    "maxParallelTasks": "integer (optional, default: 8)",
    "confidenceThreshold": "float (optional, default: 0.7)",
    "outputFormat": "string (optional, default: 'original')",
    "jpegQuality": "integer (optional, default: 90)"
  }
}
```

## Field Details

### `folders` (Required)

-   **`samples`**: Path to the directory containing your example wallpapers. The tool learns your style from these images.
-   **`candidates`**: Path to the directory containing new images you want to evaluate.
-   **`output`**: Path where qualified wallpapers will be copied.
-   **`temp`**: Path for temporary files used during image conversion and LLM analysis. This folder is automatically cleaned up.

### `llm` (Required)

-   **`endpoint`**: The base URL of your LMStudio API (e.g., `http://localhost:1234/api/v1`).
-   **`model`**: The ID of the multimodal model loaded in LMStudio.
-   **`apiKey`**: Optional API key if your LLM service requires authentication.

### `processing` (Optional)

-   **`maxParallelTasks`**: Maximum number of parallel threads for image processing and I/O. Range: 1 to 8. Default: `8`.
-   **`confidenceThreshold`**: The minimum confidence score (0.0 to 1.0) required for an image to be considered "qualified". Default: `0.7`.
-   **`outputFormat`**: The format for copied images. Options: `"original"`, `"jpeg"`, `"png"`. Default: `"original"`.
-   **`jpegQuality`**: Quality level (1-100) used when converting to JPEG. Default: `90`.

## Example: Minimal Configuration

```json
{
  "folders": {
    "samples": "/Users/user/Pictures/Favorites",
    "candidates": "/Users/user/Downloads/NewWallpapers",
    "output": "/Users/user/Pictures/Qualified",
    "temp": "./temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "llama-3-vision"
  }
}
```

## Example: Full Configuration

```json
{
  "folders": {
    "samples": "./data/samples",
    "candidates": "./data/candidates",
    "output": "./data/output",
    "temp": "./data/temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "llama-3-vision",
    "apiKey": "sk-1234567890"
  },
  "processing": {
    "maxParallelTasks": 4,
    "confidenceThreshold": 0.85,
    "outputFormat": "jpeg",
    "jpegQuality": 85
  }
}
```
