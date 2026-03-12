# Real-World Usage Scenarios

This guide provides practical examples of how to use **Wallpaper Qualifier** to curate your image collections.

---

## 🏔️ Scenario 1: Nature & Landscape Curation

You have a collection of high-resolution nature photos that you use as wallpapers, and you want to find more that match that aesthetic.

### Configuration (`nature-config.json`)

```json
{
  "folders": {
    "samples": "/Users/username/Pictures/Wallpapers/Nature-Best",
    "candidates": "/Users/username/Downloads/Unsplash-Nature-Batch",
    "output": "/Users/username/Pictures/Wallpapers/Nature-Curated",
    "temp": "./temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "llama-3-vision"
  },
  "processing": {
    "confidenceThreshold": 0.85
  }
}
```

### Execution

```bash
java -jar build/libs/wallpaper-qualifier-1.0.0-all.jar nature-config.json
```

**Outcome:** The tool will analyze your "Nature-Best" folder, identify patterns (e.g., "high saturation," "minimalist mountains," "golden hour lighting"), and only copy new images from the Unsplash batch that meet those high-quality aesthetic criteria.

---

## 🎨 Scenario 2: Minimalist Desktop Setup

You prefer minimalist, abstract wallpapers with a specific color palette (e.g., dark mode, muted blues and grays).

### Configuration (`minimalist-config.json`)

```json
{
  "folders": {
    "samples": "/Users/username/Pictures/Wallpapers/Minimalist-Favorites",
    "candidates": "/Users/username/Downloads/Random-Abstract",
    "output": "/Users/username/Pictures/Wallpapers/Minimalist-Curated",
    "temp": "./temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "moondream2"
  },
  "processing": {
    "confidenceThreshold": 0.70,
    "outputFormat": "jpeg"
  }
}
```

### Execution

```bash
java -jar build/libs/wallpaper-qualifier-1.0.0-all.jar minimalist-config.json
```

**Outcome:** The tool will prioritize images that are simple, have low visual noise, and align with your dark color scheme. All qualified images will be converted to JPEG for consistent storage.

---

## 👾 Scenario 3: Pixel Art & Retro Gaming

You are building a collection of pixel art wallpapers for a retro-themed desktop.

### Configuration (`pixel-art-config.json`)

```json
{
  "folders": {
    "samples": "/Users/username/Pictures/Wallpapers/Pixel-Art-Samples",
    "candidates": "/Users/username/Downloads/Twitter-Art-Dump",
    "output": "/Users/username/Pictures/Wallpapers/Pixel-Art-Qualified",
    "temp": "./temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "llava-v1.5-7b"
  }
}
```

### Execution

```bash
java -jar build/libs/wallpaper-qualifier-1.0.0-all.jar pixel-art-config.json
```

**Outcome:** The tool will learn the characteristics of pixel art (grid structure, limited color palettes) and filter out photographic or modern 3D art from the candidate folder.

---

## ⚙️ Advanced: Using the Tool in a Cron Job

You can set up a weekly curation task that automatically cleans up your downloads folder.

### Shell Script (`curate-weekly.sh`)

```bash
#!/bin/bash
# Curate weekly wallpapers
java -jar /path/to/wallpaper-qualifier.jar /path/to/weekly-config.json
# (Optional) Clean up candidates folder after curation
# rm /Users/username/Downloads/Wallpapers/*
```

### Cron Tab

```bash
0 0 * * 0 /Users/username/scripts/curate-weekly.sh >> /Users/username/logs/curation.log 2>&1
```
