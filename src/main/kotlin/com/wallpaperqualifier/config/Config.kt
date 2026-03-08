package com.wallpaperqualifier.config

import kotlinx.serialization.Serializable

/**
 * LLM configuration for connecting to the LM service.
 */
@Serializable
data class LLMConfig(
    val endpoint: String = "http://localhost:1234/api/v1",
    val model: String = "llama2",
    val apiKey: String? = null
)

/**
 * Processing configuration for image and batch handling.
 */
@Serializable
data class ProcessingConfig(
    val maxParallelTasks: Int = 8,
    val outputFormat: String = "original", // "original", "jpeg", "png"
    val jpegQuality: Int = 90 // 1-100
)

/**
 * Folder configuration for input/output paths.
 */
@Serializable
data class FoldersConfig(
    val samples: String,
    val candidates: String,
    val output: String,
    val temp: String
)

/**
 * Main application configuration loaded from JSON file.
 */
@Serializable
data class AppConfig(
    val folders: FoldersConfig,
    val llm: LLMConfig = LLMConfig(),
    val processing: ProcessingConfig = ProcessingConfig()
)
