package io.andromeda.fragments.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream

class BasicImageOptimizer : ImageOptimizer {
    
    private val logger = LoggerFactory.getLogger(BasicImageOptimizer::class.java)
    
    override suspend fun optimize(inputStream: InputStream, outputPath: String, options: ImageResizeOptions): Result<OptimizedImage> = withContext(Dispatchers.IO) {
        inputStream.use { stream ->
            try {
                val image = ImageIO.read(stream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
                val originalSize = stream.available().toLong()
                
                val resizedImage = resizeImage(image, options)
                
                val originalPath = stream.toString()
                
                val format = options.format.lowercase()
                val outputFormat = getOutputFormat(format)
                 
                val quality = options.quality
                val outputFile = File(outputPath)
                outputFile.parentFile?.mkdirs()
                
                writeImageWithQuality(resizedImage, outputFormat, quality, outputFile)
                
                val optimizedSize = outputFile.length()
                var sizeReduction = 0.0f
                if (originalSize > 0) {
                    sizeReduction = ((originalSize - optimizedSize).toFloat() / originalSize) * 100f
                }
                
                val metadata = ImageMetadata(
                    width = resizedImage.width,
                    height = resizedImage.height,
                    format = format,
                    sizeBytes = optimizedSize,
                    mimeType = "image/$outputFormat",
                    hasAlpha = false,
                    colorDepth = 24
                )
                
                val optimizedImage = OptimizedImage(
                    originalPath = originalPath,
                    optimizedPath = outputPath,
                    originalSize = originalSize,
                    optimizedSize = optimizedSize,
                    sizeReduction = sizeReduction,
                    width = resizedImage.width,
                    height = resizedImage.height,
                    format = format,
                    quality = quality,
                    metadata = metadata
                )
                
                return@withContext Result.success(optimizedImage)
            } catch (e: Exception) {
                logger.error("Failed to optimize image", e)
                return@withContext Result.failure(e)
            }
        }
    }
    
    override suspend fun optimize(filePath: String, options: ImageResizeOptions): Result<OptimizedImage> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File not found: $filePath"))
        }
        
        val fileName = getOptimizedFileName(filePath, options.format)
        val outputPath = file.parent + File.separator + fileName
        
        file.inputStream().use { inputStream ->
            val image = ImageIO.read(inputStream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
            val originalSize = file.length()
            
            val resizedImage = resizeImage(image, options)
            
            val format = options.format.lowercase()
            val outputFormat = getOutputFormat(format)
             
            val quality = options.quality
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            writeImageWithQuality(resizedImage, outputFormat, quality, outputFile)
            
            val optimizedSize = outputFile.length()
            var sizeReduction = 0.0f
            if (originalSize > 0) {
                sizeReduction = ((originalSize - optimizedSize).toFloat() / originalSize) * 100f
            }
            
            val metadata = ImageMetadata(
                width = resizedImage.width,
                height = resizedImage.height,
                format = format,
                sizeBytes = optimizedSize,
                mimeType = "image/$outputFormat",
                hasAlpha = false,
                colorDepth = 24
            )
            
            val optimizedImage = OptimizedImage(
                originalPath = filePath,
                optimizedPath = outputPath,
                originalSize = originalSize,
                optimizedSize = optimizedSize,
                sizeReduction = sizeReduction,
                width = resizedImage.width,
                height = resizedImage.height,
                format = format,
                quality = quality,
                metadata = metadata
            )
            
            return@withContext Result.success(optimizedImage)
        }
    }
    
    override suspend fun generateResponsiveVariants(imagePath: String, variants: List<ImageResizeOptions>): Result<List<ResponsiveVariant>> = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File not found: $imagePath"))
            }
            
            val baseName = file.nameWithoutExtension
            val parentDir = file.parent
            val variantList = mutableListOf<ResponsiveVariant>()
            
            for (index in variants.indices) {
                val opts = variants[index]
                val fileName = "${baseName}-${opts.maxWidth}x${opts.maxHeight}.${opts.format}"
                val outputPath = parentDir + File.separator + fileName
                
                FileInputStream(imagePath).use { inputStream ->
                    val result = optimize(inputStream, outputPath, opts)
                    if (result.isSuccess) {
                        val optimized = result.getOrNull()!!
                        val variant = ResponsiveVariant(
                            name = opts.maxWidth?.toString() ?: "original",
                            path = outputPath,
                            width = optimized.width,
                            height = optimized.height,
                            sizeBytes = optimized.optimizedSize,
                            format = optimized.format,
                            mediaQuery = generateMediaQuery(optimized.width)
                        )
                        variantList.add(variant)
                    }
                }
            }
            
            return@withContext Result.success(variantList.toList())
        } catch (e: Exception) {
            logger.error("Failed to generate responsive variants", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun getMetadata(imagePath: String): Result<ImageMetadata> = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File not found: $imagePath"))
            }
            
            val image = ImageIO.read(file) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
            val formatName = file.extension.lowercase()
            val mimeType = "image/$formatName"
            
            val metadata = ImageMetadata(
                width = image.width,
                height = image.height,
                format = formatName,
                sizeBytes = file.length(),
                mimeType = mimeType,
                hasAlpha = false,
                colorDepth = 24
            )
            
            return@withContext Result.success(metadata)
        } catch (e: Exception) {
            logger.error("Failed to get image metadata", e)
            return@withContext Result.failure(e)
        }
    }
    
    override suspend fun resize(imagePath: String, options: ImageResizeOptions): Result<OptimizedImage> = withContext(Dispatchers.IO) {
        optimize(imagePath, options)
    }
    
    override suspend fun convertFormat(imagePath: String, targetFormat: String): Result<OptimizedImage> = withContext(Dispatchers.IO) {
        optimize(imagePath, ImageResizeOptions(format = targetFormat))
    }
    
    override suspend fun compress(imagePath: String, quality: Float): Result<OptimizedImage> = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File not found: $imagePath"))
        }
        
        val currentFormat = file.extension.lowercase()
        optimize(imagePath, ImageResizeOptions(format = currentFormat, quality = quality))
    }
    
    private fun resizeImage(image: BufferedImage, options: ImageResizeOptions): BufferedImage {
        val originalWidth = image.width
        val originalHeight = image.height
        
        var newWidth = originalWidth
        var newHeight = originalHeight
        
        if (options.maxWidth != null && originalWidth > options.maxWidth) {
            val ratio = options.maxWidth.toFloat() / originalWidth
            newWidth = options.maxWidth
            if (options.maintainAspectRatio) {
                newHeight = (originalHeight * ratio).toInt()
            }
        }
        
        if (options.maxHeight != null && newHeight > options.maxHeight) {
            val ratio = options.maxHeight.toFloat() / newHeight
            newHeight = options.maxHeight
            if (options.maintainAspectRatio) {
                newWidth = (newWidth * ratio).toInt()
            }
        }
        
        if (newWidth != originalWidth || newHeight != originalHeight) {
            val hints = RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            
            val resized = BufferedImage(newWidth, newHeight, image.type)
            val g2d = resized.createGraphics()
            g2d.setRenderingHints(hints)
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null)
            g2d.dispose()
            
            return resized
        }
        
        return image
    }
    
    private fun getOptimizedFileName(originalPath: String, format: String): String {
        val file = File(originalPath)
        val baseName = file.nameWithoutExtension
        return "$baseName-optimized.$format"
    }
    
    private fun getOutputFormat(format: String): String {
        return when (format.lowercase()) {
            "jpg", "jpeg" -> "jpg"
            "png" -> "png"
            "webp" -> "png"
            "gif" -> "gif"
            else -> "jpg"
        }
    }

    private fun generateMediaQuery(width: Int): String {
        return when {
            width <= 480 -> "(max-width: 480px)"
            width <= 768 -> "(max-width: 768px)"
            width <= 1024 -> "(max-width: 1024px)"
            width <= 1280 -> "(max-width: 1280px)"
            width <= 1920 -> "(max-width: 1920px)"
            else -> "(max-width: ${width}px)"
        }
    }
    
    private fun writeImageWithQuality(image: BufferedImage, format: String, quality: Float, outputFile: File) {
        if (format.equals("jpg", ignoreCase = true) || format.equals("jpeg", ignoreCase = true)) {
            val writers = ImageIO.getImageWritersByFormatName("jpg")
            if (writers.hasNext()) {
                val writer = writers.next()
                val param = writer.defaultWriteParam
                if (param.canWriteCompressed()) {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = quality.coerceIn(0.0f, 1.0f)
                }
                
                FileImageOutputStream(outputFile).use { output ->
                    writer.output = output
                    writer.write(null, IIOImage(image, null, null), param)
                    writer.dispose()
                }
                return
            }
        }
        
        ImageIO.write(image, format, outputFile)
    }
}
