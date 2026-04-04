# Fragments Image Optimization Core

Image resizing, format conversion, compression, and responsive variant generation.

## Usage

```kotlin
val optimizer: ImageOptimizer = BasicImageOptimizer()

// Resize an image
val result = optimizer.resize("cover.jpg", ImageResizeOptions(
    maxWidth = 800,
    maxHeight = 600,
    quality = 0.85f,
    format = "jpg"
))

// Generate responsive variants
val variants = optimizer.generateResponsiveVariants("hero.jpg", listOf(
    ImageResizeOptions(maxWidth = 320, format = "webp"),
    ImageResizeOptions(maxWidth = 768, format = "webp"),
    ImageResizeOptions(maxWidth = 1200, format = "webp")
))

// Get image metadata
val metadata = optimizer.getMetadata("photo.jpg")
// metadata.width, metadata.height, metadata.format, metadata.sizeBytes

// Convert format
val webp = optimizer.convertFormat("photo.png", targetFormat = "webp")

// Compress
val compressed = optimizer.compress("large.jpg", quality = 0.7f)
```
