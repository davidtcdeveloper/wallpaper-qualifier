package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.QualityProfile

/**
 * Centralized prompt templates for LLM interactions.
 *
 * Versioned so changes can be tracked and tests remain stable.
 */
object PromptTemplates {

    const val VERSION: String = "v1"

    fun sampleAnalysisPrompt(): String =
        """
        You are an expert wallpaper analyst. Analyze the provided wallpaper image
        and describe its key aesthetic and technical characteristics in strict JSON format.

        Focus on:
        1. Color palette (primary and accent colors with hex codes)
        2. Visual style (minimalist, vibrant, abstract, photographic, etc.)
        3. Mood/atmosphere (calming, energetic, dramatic, contemplative, etc.)
        4. Composition (centered, rule_of_thirds, symmetrical, dynamic, etc.)
        5. Subject matter (nature, urban, abstract, art, patterns, etc.)
        6. Technical quality (resolution, focus, lighting, clarity 0-1)

        REQUIRED OUTPUT FORMAT (JSON ONLY):
        {
          "colorPalette": ["#112233", "#445566", "#778899"],
          "style": "minimalist",
          "mood": "calming",
          "composition": "rule_of_thirds",
          "subject": "nature",
          "technicalNotes": "High resolution, sharp focus, well balanced lighting.",
          "quality": 0.92
        }

        Do not include any text outside the JSON object.
        """.trimIndent()

    fun candidateEvaluationPrompt(profile: QualityProfile): String =
        """
        You are a wallpaper curation expert. Evaluate whether this candidate wallpaper
        matches the user's preferences based on their quality profile.

        USER QUALITY PROFILE SUMMARY:
        ${profileSummary(profile)}

        REQUIRED OUTPUT FORMAT (JSON ONLY):
        {
          "qualified": true,
          "confidenceScore": 0.85,
          "reasoning": "Matches color palette and mood preferences."
        }

        Rules:
        - confidenceScore must be between 0.0 and 1.0
        - Be critical: if the match is weak or ambiguous, set "qualified" to false.
        - Do not include any text outside the JSON object.
        """.trimIndent()

    private fun profileSummary(profile: QualityProfile): String =
        """
        Preferred color palettes: ${profile.preferredColorPalettes.joinToString()}
        Preferred styles: ${profile.preferredStyles.joinToString()}
        Preferred moods: ${profile.preferredMoods.joinToString()}
        Preferred compositions: ${profile.preferredCompositions.joinToString()}
        Common subjects: ${profile.commonSubjects.joinToString()}
        Resolution preferences: min ${profile.resolutionPreferences.minWidth}x${profile.resolutionPreferences.minHeight},
        max ${profile.resolutionPreferences.maxWidth}x${profile.resolutionPreferences.maxHeight}
        Average sample quality: ${profile.averageQuality}
        Sample count: ${profile.sampleCount}
        """.trimIndent()
}

