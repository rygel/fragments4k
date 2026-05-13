# Fragments Image Optimization Core

Image resizing, format conversion, compression, and responsive variant generation.

## Supported Formats

- **JPEG** (`jpg`, `jpeg`)
- **PNG** (`png`)
- **GIF** (`gif`)

WebP, BMP, TIFF, and other formats are not supported. Requests for unsupported formats fail with `IllegalArgumentException`.

## Input Validation

- `quality` must be between `0.0` and `1.0`
- `maxWidth` and `maxHeight` must be positive integers (if specified)
- Images exceeding 20000x20000 pixels are rejected

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
    ImageResizeOptions(maxWidth = 320, format = "png"),
    ImageResizeOptions(maxWidth = 768, format = "png"),
    ImageResizeOptions(maxWidth = 1200, format = "jpg")
))

// Get image metadata
val metadata = optimizer.getMetadata("photo.jpg")
// metadata.width, metadata.height, metadata.format, metadata.sizeBytes

// Convert format
val png = optimizer.convertFormat("photo.jpg", targetFormat = "png")

// Compress
val compressed = optimizer.compress("large.jpg", quality = 0.7f)
```
