package com.techcity.techcityassist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Manages local caching of phone images.
 * Images are stored in the app's internal files directory under "phone_images/"
 *
 * File naming convention: {phoneDocId}_{colorName}_{resolution}.png
 * Example: ia2JilFWjlB2su72xY56_black_low.png
 */
object ImageCacheManager {

    private const val TAG = "ImageCacheManager"
    private const val CACHE_DIR = "phone_images"

    /**
     * Get the cache directory, creating it if necessary
     */
    private fun getCacheDir(context: Context): File {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Generate a consistent filename for a phone image
     */
    private fun getFileName(phoneDocId: String, colorName: String, isHighRes: Boolean): String {
        val sanitizedColor = colorName.lowercase().replace(" ", "_")
        val resolution = if (isHighRes) "high" else "low"
        return "${phoneDocId}_${sanitizedColor}_$resolution.png"
    }

    /**
     * Get the local file path for a phone image
     */
    fun getLocalFilePath(context: Context, phoneDocId: String, colorName: String, isHighRes: Boolean): File {
        val fileName = getFileName(phoneDocId, colorName, isHighRes)
        return File(getCacheDir(context), fileName)
    }

    /**
     * Check if an image exists locally
     */
    fun isImageCached(context: Context, phoneDocId: String, colorName: String, isHighRes: Boolean): Boolean {
        val file = getLocalFilePath(context, phoneDocId, colorName, isHighRes)
        return file.exists() && file.length() > 0
    }

    /**
     * Get the local image URI if it exists, otherwise return null
     */
    fun getLocalImageUri(context: Context, phoneDocId: String, colorName: String, isHighRes: Boolean): String? {
        val file = getLocalFilePath(context, phoneDocId, colorName, isHighRes)
        return if (file.exists() && file.length() > 0) {
            file.absolutePath
        } else {
            null
        }
    }

    /**
     * Download and cache an image from a URL
     * Returns the local file path if successful, null otherwise
     */
    suspend fun downloadAndCacheImage(
        context: Context,
        imageUrl: String,
        phoneDocId: String,
        colorName: String,
        isHighRes: Boolean
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = getLocalFilePath(context, phoneDocId, colorName, isHighRes)

            // If already cached, return the path
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "Image already cached: ${file.name}")
                return@withContext file.absolutePath
            }

            Log.d(TAG, "Downloading image: $imageUrl")

            // Download the image
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                // Save to local file
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                bitmap.recycle()

                Log.d(TAG, "Image cached successfully: ${file.name}")
                return@withContext file.absolutePath
            } else {
                Log.e(TAG, "Failed to decode bitmap from URL: $imageUrl")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading/caching image: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Get the best available image source - local if cached, otherwise remote URL
     * Also triggers background download if not cached
     */
    suspend fun getImageSource(
        context: Context,
        remoteUrl: String?,
        phoneDocId: String,
        colorName: String,
        isHighRes: Boolean
    ): String? {
        if (remoteUrl.isNullOrEmpty()) return null

        // Check if we have a local copy
        val localPath = getLocalImageUri(context, phoneDocId, colorName, isHighRes)
        if (localPath != null) {
            Log.d(TAG, "Using cached image: $localPath")
            return localPath
        }

        // Download and cache, then return the local path
        val cachedPath = downloadAndCacheImage(context, remoteUrl, phoneDocId, colorName, isHighRes)
        return cachedPath ?: remoteUrl  // Fall back to remote URL if download fails
    }

    /**
     * Preload images for a phone (all colors)
     * Call this in the background to cache images ahead of time
     */
    suspend fun preloadPhoneImages(
        context: Context,
        phoneDocId: String,
        phoneImages: PhoneImages?
    ) = withContext(Dispatchers.IO) {
        if (phoneImages == null) return@withContext

        phoneImages.colors.forEach { (colorName, colorImages) ->
            // Prefer low-res for initial display, but also cache high-res
            if (colorImages.lowRes.isNotEmpty()) {
                downloadAndCacheImage(context, colorImages.lowRes, phoneDocId, colorName, false)
            }
            if (colorImages.highRes.isNotEmpty()) {
                downloadAndCacheImage(context, colorImages.highRes, phoneDocId, colorName, true)
            }
        }
    }

    /**
     * Clear all cached images (useful for freeing up space)
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = getCacheDir(context)
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cache cleared successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}", e)
            false
        }
    }

    /**
     * Get the total size of cached images in bytes
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = getCacheDir(context)
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Get cache size as a formatted string (e.g., "12.5 MB")
     */
    fun getCacheSizeFormatted(context: Context): String {
        val bytes = getCacheSize(context)
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}