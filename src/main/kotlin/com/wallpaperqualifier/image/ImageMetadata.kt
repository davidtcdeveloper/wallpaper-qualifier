package com.wallpaperqualifier.image

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Metadata extracted from an image file
 */
@Serializable
data class ImageMetadata(
    val path: String,
    val width: Int,
    val height: Int,
    val format: String,
    val colorDepth: Int? = null,
    val dpi: Int? = null,
    val bitDepth: Int? = null,
    val orientation: Int? = null,
    val fileSize: Long? = null,
    val modificationDate: Long? = null
) {
    val aspectRatio: Double
        get() = width.toDouble() / height.toDouble()

    val filename: String
        get() = File(path).name
}
