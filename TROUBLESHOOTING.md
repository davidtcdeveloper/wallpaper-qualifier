# Troubleshooting Guide

Common issues and solutions for Wallpaper Qualifier.

## LMStudio Connection Issues

**Error:** `Failed to connect to LLM endpoint`

**Solutions:**
1.  **Check if LMStudio is running:** Ensure the LMStudio application is open.
2.  **Verify the API is active:** Go to the "Local Server" tab in LMStudio and make sure the server is "Started".
3.  **Check the endpoint:** Verify that your `config.json` endpoint matches the URL shown in LMStudio (usually `http://localhost:1234/api/v1`).
4.  **Firewall:** Ensure your firewall is not blocking internal connections to port 1234.

---

## Model Errors

**Error:** `LLM request timed out` or `Invalid response from LLM`

**Solutions:**
1.  **Check Model Type:** Ensure you have loaded a **multimodal** (vision-capable) model in LMStudio. Standard text-only models cannot analyze images.
2.  **System Resources:** Large vision models require significant RAM and GPU/CPU power. If your system is under heavy load, the request might time out. Increase the timeout in your configuration if necessary.
3.  **Model Compatibility:** Some models might use a different JSON response format. Ensure your model is compatible with OpenAI-style chat completions.

---

## Image Loading Issues

**Error:** `No valid images found` or `Failed to load image`

**Solutions:**
1.  **Check Permissions:** Ensure the tool has read access to your `samples` and `candidates` folders, and write access to `output` and `temp`.
2.  **Supported Formats:** Verify your images are in supported formats (JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW).
3.  **Corrupted Files:** If a file is corrupted or partially downloaded, the tool will skip it.

---

## Performance Issues

**Problem:** Workflow is slow.

**Solutions:**
1.  **Parallel Tasks:** Increase `maxParallelTasks` in `config.json` (max 8). This speeds up image conversion but uses more CPU/RAM.
2.  **Large Batches:** If you have thousands of candidates, the tool processes them in batches. Ensure you have enough disk space in your `temp` folder.
3.  **LLM Inference Time:** Local AI analysis is slow by nature. Higher-end GPUs will significantly speed up the process.

---

## Out of Memory

**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solutions:**
1.  **Increase Heap Size:** Run the tool with more memory:
    ```bash
    java -Xmx4g -jar wallpaper-qualifier.jar config.json
    ```
2.  **Reduce Parallelism:** Lower `maxParallelTasks` to 2 or 4 to reduce concurrent memory usage during image conversion.
