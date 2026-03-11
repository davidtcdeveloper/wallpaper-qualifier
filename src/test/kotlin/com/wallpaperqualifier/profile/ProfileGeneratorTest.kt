package com.wallpaperqualifier.profile

import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.ResolutionRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProfileGeneratorTest : FunSpec({

    val generator = ProfileGenerator()

    test("should aggregate empty list into empty profile") {
        val profile = generator.aggregate(emptyList())
        profile.sampleCount shouldBe 0
        profile.preferredColorPalettes shouldBe emptyList()
        profile.averageQuality shouldBe 0f
    }

    test("should aggregate characteristics by frequency") {
        val characteristics = listOf(
            ImageCharacteristics(
                colorPalette = listOf("#000000", "#FFFFFF"),
                style = "Minimalist",
                mood = "Calm",
                composition = "Centered",
                subject = "Nature",
                technicalNotes = "",
                quality = 0.9f
            ),
            ImageCharacteristics(
                colorPalette = listOf("#000000", "#FF0000"),
                style = "Minimalist",
                mood = "Energetic",
                composition = "Rule of Thirds",
                subject = "Nature",
                technicalNotes = "",
                quality = 0.7f
            ),
            ImageCharacteristics(
                colorPalette = listOf("#FFFFFF"),
                style = "Abstract",
                mood = "Calm",
                composition = "Centered",
                subject = "Digital",
                technicalNotes = "",
                quality = 0.8f
            )
        )

        val profile = generator.aggregate(characteristics)

        profile.sampleCount shouldBe 3
        profile.averageQuality shouldBe 0.8f
        
        // Colors: #000000 (2), #FFFFFF (2), #FF0000 (1)
        profile.preferredColorPalettes.take(2) shouldContainExactly listOf("#000000", "#FFFFFF")
        
        // Styles: Minimalist (2), Abstract (1)
        profile.preferredStyles shouldBe listOf("Minimalist", "Abstract")
        
        // Moods: Calm (2), Energetic (1)
        profile.preferredMoods shouldBe listOf("Calm", "Energetic")
        
        // Compositions: Centered (2), Rule of Thirds (1)
        profile.preferredCompositions shouldBe listOf("Centered", "Rule of Thirds")
        
        // Subjects: Nature (2), Digital (1)
        profile.commonSubjects shouldBe listOf("Nature", "Digital")
    }

    test("should respect resolution range parameter") {
        val range = ResolutionRange(minWidth = 3840, minHeight = 2160)
        val profile = generator.aggregate(emptyList(), range)
        profile.resolutionPreferences shouldBe range
    }
})
