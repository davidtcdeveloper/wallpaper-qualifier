package com.wallpaperqualifier.profile

import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.ResolutionRange

/**
 * Aggregates a list of image characteristics into a single quality profile.
 * Identifies the most frequent aesthetic and technical characteristics from samples.
 */
class ProfileGenerator {

    /**
     * Aggregate multiple image characteristics into a single quality profile.
     *
     * @param characteristics List of characteristics from sample images
     * @param resolutionRange Desired resolution preferences for the profile
     * @return QualityProfile representing the aggregated characteristics
     */
    fun aggregate(
        characteristics: List<ImageCharacteristics>,
        resolutionRange: ResolutionRange = ResolutionRange()
    ): QualityProfile {
        if (characteristics.isEmpty()) {
            return QualityProfile(
                preferredColorPalettes = emptyList(),
                preferredStyles = emptyList(),
                preferredMoods = emptyList(),
                preferredCompositions = emptyList(),
                commonSubjects = emptyList(),
                resolutionPreferences = resolutionRange,
                averageQuality = 0f,
                sampleCount = 0
            )
        }

        return QualityProfile(
            preferredColorPalettes = selectTop(characteristics.flatMap { it.colorPalette }, 10),
            preferredStyles = selectTop(characteristics.map { it.style }, 5),
            preferredMoods = selectTop(characteristics.map { it.mood }, 5),
            preferredCompositions = selectTop(characteristics.map { it.composition }, 3),
            commonSubjects = selectTop(characteristics.map { it.subject }, 5),
            resolutionPreferences = resolutionRange,
            averageQuality = characteristics.map { it.quality }.average().toFloat(),
            sampleCount = characteristics.size
        )
    }

    /**
     * Select the top N most frequent items from a list.
     */
    private fun <T> selectTop(items: List<T>, n: Int): List<T> {
        return items.groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(n)
            .map { it.first }
    }
}
