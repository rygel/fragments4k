package io.andromeda.fragments.image

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class BasicImageOptimizerTest {

    private lateinit var optimizer: BasicImageOptimizer

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        optimizer = BasicImageOptimizer()
    }

    @Test
    fun optimizeCreatesOptimizedImage() = runBlocking {
        val testImage = createTestImage(800, 600)
        val imagePath = testImage.absolutePath
        
        val result = optimizer.optimize(imagePath, ImageResizeOptions(maxWidth = 400))
        
        assertTrue(result.isSuccess)
        val optimizedImage = result.getOrNull()!!
        
        assertNotNull(optimizedImage.originalPath)
        assertTrue(File(optimizedImage.optimizedPath).exists())
        assertTrue(optimizedImage.optimizedSize > 0)
        assertTrue(optimizedImage.optimizedSize < optimizedImage.originalSize)
        assertTrue(optimizedImage.width <= 400)
        assertTrue(optimizedImage.height <= 400)
        assertTrue(optimizedImage.compressionRatio <= 1.0f)
    }

    @Test
    fun getMetadataReturnsCorrectInfo() = runBlocking {
        val testImage = createTestImage(800, 600)
        val imagePath = File(tempDir, "test.jpg").absolutePath
        
        testImage.writeBytes(createImageData(800, 600))
        
        val result = optimizer.getMetadata(imagePath)
        
        assertTrue(result.isSuccess)
        val metadata = result.getOrNull()!!
        
        assertEquals(800, metadata.width)
        assertEquals(600, metadata.height)
        assertTrue(metadata.sizeBytes > 0)
        assertEquals("jpg", metadata.format)
        assertEquals("image/jpg", metadata.mimeType)
    }

    @Test
    fun generateResponsiveVariantsCreatesMultipleSizes() = runBlocking {
        val testImage = createTestImage(1920, 1080)
        val imagePath = File(tempDir, "test.jpg").absolutePath
        
        testImage.writeBytes(createImageData(1920, 1080))
        
        val variants = listOf(
            ImageResizeOptions(maxWidth = 400, maxHeight = 400),
            ImageResizeOptions(maxWidth = 800, maxHeight = 800),
            ImageResizeOptions(maxWidth = 1920, maxHeight = 1920)
        )
        
        val result = optimizer.generateResponsiveVariants(imagePath, variants)
        
        assertTrue(result.isSuccess)
        val generatedVariants = result.getOrNull()!!
        
        assertEquals(3, generatedVariants.size)
        
        val smallVariant = generatedVariants.find { it.name == "400" }
        assertNotNull(smallVariant)
        assertTrue(smallVariant!!.width <= 400)
        
        val mediumVariant = generatedVariants.find { it.name == "800" }
        assertNotNull(mediumVariant)
        assertTrue(mediumVariant!!.width <= 800)
        
        val largeVariant = generatedVariants.find { it.name == "1920" }
        assertNotNull(largeVariant)
        assertTrue(largeVariant!!.width <= 1920)
    }

    @Test
    fun maintainAspectRatio() = runBlocking {
        val testImage = createTestImage(800, 600)
        val outputPath = File(tempDir, "optimized.jpg").absolutePath
        
        val inputStream = testImage.inputStream()
        val result = optimizer.optimize(inputStream, outputPath, ImageResizeOptions(maxWidth = 400, maintainAspectRatio = true))
        
        assertTrue(result.isSuccess)
        val optimizedImage = result.getOrNull()!!
        
        val aspectRatio = 800.0f / 600.0f
        val optimizedAspectRatio = optimizedImage.width.toFloat() / optimizedImage.height.toFloat()
        
        val ratioDifference = Math.abs(aspectRatio - optimizedAspectRatio)
        assertTrue(ratioDifference < 0.01f)
    }

    @Test
    fun qualityParameterAffectsSize() = runBlocking {
        val testImage = createTestImage(800, 600)
        
        val highQualityPath = File(tempDir, "high-quality.jpg").absolutePath
        val lowQualityPath = File(tempDir, "low-quality.jpg").absolutePath
        
        testImage.writeBytes(createImageData(800, 600))
        
        val highQualityResult = optimizer.optimize(
            testImage.absolutePath,
            ImageResizeOptions(maxWidth = 400, quality = 0.9f)
        )
        
        val lowQualityResult = optimizer.optimize(
            testImage.absolutePath,
            ImageResizeOptions(maxWidth = 400, quality = 0.5f)
        )
        
        assertTrue(highQualityResult.isSuccess)
        assertTrue(lowQualityResult.isSuccess)
        
        val highQuality = highQualityResult.getOrNull()!!
        val lowQuality = lowQualityResult.getOrNull()!!
        
        assertTrue(lowQuality.optimizedSize < highQuality.optimizedSize)
        assertTrue(lowQuality.quality < highQuality.quality)
    }

    @Test
    fun convertFormat() = runBlocking {
        val testImage = createTestImage(800, 600)
        val imagePath = File(tempDir, "test.jpg").absolutePath
        
        testImage.writeBytes(createImageData(800, 600))
        
        val result = optimizer.convertFormat(imagePath, "png")
        
        assertTrue(result.isSuccess)
        val optimizedImage = result.getOrNull()!!
        
        assertEquals("png", optimizedImage.format)
        assertTrue(File(optimizedImage.optimizedPath).exists())
    }

    @Test
    fun compress() = runBlocking {
        val testImage = createTestImage(800, 600)
        val imagePath = File(tempDir, "test.jpg").absolutePath
        
        testImage.writeBytes(createImageData(800, 600))
        
        val result = optimizer.compress(imagePath, 0.7f)
        
        assertTrue(result.isSuccess)
        val optimizedImage = result.getOrNull()!!
        
        assertEquals(0.7f, optimizedImage.quality)
        assertTrue(optimizedImage.sizeReduction > 0)
    }

    @Test
    fun optimizeWithPresetOptions() = runBlocking {
        val testImage = createTestImage(800, 600)
        val testImageFile = File(tempDir, "test-preset.jpg")
        testImageFile.writeBytes(createImageData(800, 600))
        
        val result = optimizer.optimize(testImageFile.absolutePath, ImageResizeOptions.THUMBNAIL)
        
        assertTrue(result.isSuccess)
        val optimizedImage = result.getOrNull()!!
        
        assertTrue(optimizedImage.width <= 200)
        assertTrue(optimizedImage.height <= 200)
        assertTrue(optimizedImage.format == "jpg")
    }

    @Test
    fun optimizeReturnsErrorForInvalidInput() = runBlocking {
        val invalidPath = File(tempDir, "nonexistent.jpg").absolutePath
        
        val result = optimizer.optimize(invalidPath, ImageResizeOptions())
        
        assertTrue(result.isFailure)
    }

    @Test
    fun generateMediaQueryForDifferentWidths() = runBlocking {
        val optimizer = BasicImageOptimizer()
        
        val width = 480
        val method = BasicImageOptimizer::class.java.getDeclaredMethod("generateMediaQuery", Int::class.java)
        method.isAccessible = true
        
        val query = method.invoke(optimizer, width) as String
        
        assertNotNull(query)
        assertTrue(query.contains("max-width"))
        assertTrue(query.contains("$width"))
    }

    private fun createTestImage(width: Int, height: Int): File {
        val file = File(tempDir, "test-image.jpg")
        file.writeBytes(createImageData(width, height))
        return file
    }

    private fun createImageData(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        val g2d = image.createGraphics()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val r = ((x.toFloat() / width) * 255).toInt()
                val g = ((y.toFloat() / height) * 255).toInt()
                val b = ((x + y).toFloat() / (width + height) * 255).toInt()
                val rgb = (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, rgb)
            }
        }
        g2d.dispose()
        
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", outputStream)
        return outputStream.toByteArray()
    }
}
