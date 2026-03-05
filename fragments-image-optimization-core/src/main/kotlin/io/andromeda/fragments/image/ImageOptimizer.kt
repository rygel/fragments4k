package io.andromeda.fragments.image

import java.io.InputStream

data class ImageMetadata(
    val width: Int,
    val height: Int,
    val format: String,
    val sizeBytes: Long,
    val mimeType: String,
    val orientation: Int? = null,
    val hasAlpha: Boolean = false,
    val colorDepth: Int? = null,
    val xDPI: Float? = null,
    val yDPI: Float? = null
)

data class OptimizedImage(
    val originalPath: String,
    val optimizedPath: String,
    val originalSize: Long,
    val optimizedSize: Long,
    val sizeReduction: Float,
    val width: Int,
    val height: Int,
    val format: String,
    val quality: Float,
    val metadata: ImageMetadata
) {
    val compressionRatio: Float
        get() {
            if (originalSize > 0) {
                return optimizedSize.toFloat() / originalSize
            }
            return 1.0f
        }

    val spaceSaved: Long
        get() = originalSize - optimizedSize
}

data class ImageResizeOptions(
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val maintainAspectRatio: Boolean = true,
    val quality: Float = 0.85f,
    val format: String = "jpg"
) {
    companion object {
        val THUMBNAIL = ImageResizeOptions(maxWidth = 200, maxHeight = 200, quality = 0.7f)
        val MEDIUM = ImageResizeOptions(maxWidth = 800, maxHeight = 800, quality = 0.8f)
        val LARGE = ImageResizeOptions(maxWidth = 1920, maxHeight = 1080, quality = 0.85f)
        val RETINA = ImageResizeOptions(maxWidth = 3840, maxHeight = 2160, quality = 0.9f)
    }
}

data class ResponsiveVariant(
    val name: String,
    val path: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val format: String,
    val mediaQuery: String
)

interface ImageOptimizer {
    suspend fun optimize(inputStream: InputStream, outputPath: String, options: ImageResizeOptions): Result<OptimizedImage>
    suspend fun optimize(filePath: String, options: ImageResizeOptions): Result<OptimizedImage>
    suspend fun generateResponsiveVariants(imagePath: String, variants: List<ImageResizeOptions>): Result<List<ResponsiveVariant>>
    suspend fun getMetadata(imagePath: String): Result<ImageMetadata>
    suspend fun resize(imagePath: String, options: ImageResizeOptions): Result<OptimizedImage>
    suspend fun convertFormat(imagePath: String, targetFormat: String): Result<OptimizedImage>
    suspend fun compress(imagePath: String, quality: Float): Result<OptimizedImage>
}
