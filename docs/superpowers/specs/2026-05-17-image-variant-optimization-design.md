# Image Variant Generation Optimization Design

**Date:** 2026-05-17
**Status:** Draft
**Scope:** Single task: optimize `generateResponsiveVariants()` to decode source image once

## Overview

`BasicImageOptimizer.generateResponsiveVariants()` opens and decodes the source image file separately for each responsive variant. For a typical configuration with 4 preset sizes (thumbnail, medium, large, retina), the source image is fully decoded 4 times. This fix decodes once into a `BufferedImage` and derives all variants from the in-memory pixel data.

## Current Behavior

```kotlin
override suspend fun generateResponsiveVariants(...) {
    for (opts in variants) {
        FileInputStream(imagePath).use { stream ->      // open once per variant
            optimize(stream, outputPath, opts)            // each call does ImageIO.read + resize
        }
    }
}
```

`optimize(inputStream, ...)` reads the image from the stream every time. The source JPG/PNG is fully decompressed, resized, and re-encoded for each variant independently.

## Design

**No API surface changes.** Only internal refactoring of `BasicImageOptimizer`.

### New private methods

```kotlin
private fun readImage(path: String): BufferedImage
```

Reads the source image file once, applying preflight checks. Returns a `BufferedImage`.

```kotlin
private fun generateVariantsFromImage(
    source: BufferedImage,
    baseName: String,
    parentDir: String,
    variants: List<ImageResizeOptions>,
): List<ResponsiveVariant>
```

Takes a pre-decoded `BufferedImage`, iterates over variants using `resizeImage()` + `writeImageWithQuality()` directly — no file I/O for the decode step.

### Updated `generateResponsiveVariants`

```kotlin
override suspend fun generateResponsiveVariants(
    imagePath: String,
    variants: List<ImageResizeOptions>,
): Result<List<ResponsiveVariant>> {
    val file = File(imagePath)
    if (!file.exists()) return failure
    val source = decodeAndPreflight(file)           // decode once
    val results = generateVariantsFromImage(source, ...) // derive all variants
    return success(results)
}
```

### Interaction with `resizeImage`

`resizeImage()` already accepts `BufferedImage` and returns `BufferedImage` — no changes needed. Each variant calls:
1. `resizeImage(source, opts)` — derives new `BufferedImage`
2. `writeImageWithQuality(resized, format, quality, outputFile)` — writes to disk

## Acceptance Criteria

- `generateResponsiveVariants()` with 4 preset variants decodes the source image exactly once (not 4 times)
- All existing `BasicImageOptimizerTest` tests pass without modification
- Output variant files are identical in dimensions and quality to the current behavior
- Preflight checks (dimension limits, pixel count) still apply
- No new public API methods

## Out of Scope

- Thread-safe caching of decoded images across calls (single-flight pattern already handled by callers)
- Adding WebP/AVIF format support
- Changing the `ResponsiveVariant` data class
