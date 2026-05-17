# Image Variant Generation Optimization Implementation Plan

> **For agentic workers:** Single-task plan — inline execution recommended.

**Goal:** Optimize `generateResponsiveVariants()` to decode the source image once and derive all variants from the in-memory `BufferedImage`.

**Architecture:** No API surface changes. Extract image reading + preflight into a private `readAndPreflight()` method. Add private `generateVariantsFromImage()` that takes a `BufferedImage`. The existing `generateResponsiveVariants()` decodes once, then delegates.

**Tech Stack:** Kotlin/JVM, Java ImageIO, Maven, JUnit 5

---

## File Structure

- Modify: `fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt`

No new files. No test changes needed — existing tests verify correctness.

---

### Task 1: Refactor `generateResponsiveVariants` to decode once

**Files:**
- Modify: `fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt`

- [ ] **Step 1: Add private `readAndPreflight()` method**

```kotlin
private fun readAndPreflight(file: File): BufferedImage {
    file.inputStream().use { stream ->
        try {
            preflightImage(stream)
        } catch (e: IllegalArgumentException) {
            throw e
        }
    }
    return ImageIO.read(file)
        ?: throw IllegalArgumentException("Could not read image: ${file.path}")
}
```

- [ ] **Step 2: Add private `generateVariantsFromImage()` method**

```kotlin
private fun generateVariantsFromImage(
    source: BufferedImage,
    baseName: String,
    parentDir: String,
    variants: List<ImageResizeOptions>,
): List<ResponsiveVariant> {
    val variantList = mutableListOf<ResponsiveVariant>()
    for (opts in variants) {
        val fileName = "$baseName-${opts.maxWidth}x${opts.maxHeight}.${opts.format}"
        val outputPath = "$parentDir${File.separator}$fileName"
        val resized = resizeImage(source, opts)
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val quality = opts.quality
        val format = opts.format.lowercase()
        val outputFormat = getOutputFormat(format)
        writeImageWithQuality(resized, outputFormat, quality, outputFile)
        val variant = ResponsiveVariant(
            name = opts.maxWidth?.toString() ?: "original",
            path = outputPath,
            width = resized.width,
            height = resized.height,
            sizeBytes = outputFile.length(),
            format = format,
            mediaQuery = generateMediaQuery(resized.width),
        )
        variantList.add(variant)
    }
    return variantList.toList()
}
```

- [ ] **Step 3: Rewrite `generateResponsiveVariants()` to decode once**

Replace the existing method:

```kotlin
override suspend fun generateResponsiveVariants(
    imagePath: String,
    variants: List<ImageResizeOptions>,
): Result<List<ResponsiveVariant>> =
    withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File not found: $imagePath"))
            }
            val source = readAndPreflight(file)
            val baseName = file.nameWithoutExtension
            val parentDir = file.parent
            val results = generateVariantsFromImage(source, baseName, parentDir, variants)
            return@withContext Result.success(results)
        } catch (e: IOException) {
            logger.error("Failed to generate responsive variants", e)
            return@withContext Result.failure(e)
        }
    }
```

- [ ] **Step 4: Run existing tests to verify nothing is broken**

Run: `mvn -pl fragments-image-optimization-core test -T 4`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt
git commit -m "perf: decode source image once in generateResponsiveVariants

Previously each variant re-opened and re-decoded the source file.
Now the image is decoded once into a BufferedImage and all variants
are derived from the in-memory pixel data."
```
