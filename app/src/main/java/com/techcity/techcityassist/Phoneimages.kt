package com.techcity.techcityassist

/**
 * Represents image URLs and optional custom hex color for a single color variant
 */
data class ColorImages(
    val highRes: String = "",
    val lowRes: String = "",
    val hexColor: String = ""  // Optional: Custom hex color (e.g., "#FF5733"). If empty, uses default color mapping.
)

/**
 * Represents the phone_images collection document
 * Document ID should match the corresponding phone document ID in the phones collection
 */
data class PhoneImages(
    val phoneDocId: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val colors: Map<String, ColorImages> = emptyMap()
) {
    /**
     * Get image URLs and hex color for a specific color
     * @param colorName The color name (e.g., "black", "titanium blue")
     * @return ColorImages if found, null otherwise
     */
    fun getImagesForColor(colorName: String): ColorImages? {
        // Try exact match first (case-insensitive)
        colors.entries.find { it.key.equals(colorName, ignoreCase = true) }?.let {
            return it.value
        }

        // Try with underscores replaced by spaces
        val normalizedKey = colorName.lowercase().replace(" ", "_")
        colors.entries.find { it.key.lowercase().replace(" ", "_") == normalizedKey }?.let {
            return it.value
        }

        // Try with spaces replaced by underscores
        val spacedKey = colorName.lowercase().replace("_", " ")
        colors.entries.find { it.key.lowercase().replace("_", " ") == spacedKey }?.let {
            return it.value
        }

        return null
    }

    /**
     * Get all available color names
     */
    fun getAvailableColors(): List<String> {
        return colors.keys.toList()
    }

    /**
     * Get hex color for a specific color, or null if not set
     * @param colorName The color name
     * @return Hex color string (e.g., "#FF5733") or null if not specified
     */
    fun getHexColorForColor(colorName: String): String? {
        val colorImages = getImagesForColor(colorName)
        return if (colorImages?.hexColor?.isNotEmpty() == true) {
            colorImages.hexColor
        } else {
            null
        }
    }
}