package com.techcity.techcityassist

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing phone images in Firebase Storage and Firestore
 */
class PhoneImagesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "PhoneImagesRepo"
        private const val COLLECTION_NAME = "phone_images"
        private const val STORAGE_PATH = "phone_images"
    }

    /**
     * Fetch PhoneImages document from Firestore
     * @param phoneDocId The document ID (should match the phones collection document ID)
     * @return PhoneImages if found, null otherwise
     */
    suspend fun getPhoneImages(phoneDocId: String): PhoneImages? {
        return try {
            val document = firestore.collection(COLLECTION_NAME)
                .document(phoneDocId)
                .get()
                .await()

            if (document.exists()) {
                parsePhoneImagesDocument(document.id, document.data)
            } else {
                Log.d(TAG, "No phone_images document found for: $phoneDocId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching phone images for $phoneDocId", e)
            null
        }
    }

    /**
     * Fetch PhoneImages for multiple phone document IDs
     * @param phoneDocIds List of phone document IDs
     * @return Map of phoneDocId to PhoneImages
     */
    suspend fun getPhoneImagesForMultiple(phoneDocIds: List<String>): Map<String, PhoneImages> {
        val result = mutableMapOf<String, PhoneImages>()

        // Firestore 'in' queries are limited to 30 items, so we batch if needed
        phoneDocIds.chunked(30).forEach { batch ->
            try {
                val querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereIn("phoneDocId", batch)
                    .get()
                    .await()

                for (document in querySnapshot.documents) {
                    val phoneImages = parsePhoneImagesDocument(document.id, document.data)
                    if (phoneImages != null) {
                        result[phoneImages.phoneDocId] = phoneImages
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching batch of phone images", e)
            }
        }

        return result
    }

    /**
     * Upload an image to Firebase Storage and get the download URL
     * @param phoneDocId The phone document ID (used as folder name)
     * @param colorName The color name (e.g., "black")
     * @param isHighRes Whether this is a high resolution image
     * @param imageUri The local URI of the image to upload
     * @return Download URL if successful, null otherwise
     */
    suspend fun uploadImage(
        phoneDocId: String,
        colorName: String,
        isHighRes: Boolean,
        imageUri: Uri
    ): String? {
        return try {
            val resolution = if (isHighRes) "high" else "low"
            val fileName = "${colorName.lowercase().replace(" ", "_")}_$resolution.png"
            val storagePath = "$STORAGE_PATH/$phoneDocId/$fileName"

            val storageRef = storage.reference.child(storagePath)

            // Upload the file
            storageRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Log.d(TAG, "Successfully uploaded image: $storagePath")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            null
        }
    }

    /**
     * Create or update a phone_images document in Firestore
     * @param phoneImages The PhoneImages object to save
     * @return true if successful, false otherwise
     */
    suspend fun savePhoneImages(phoneImages: PhoneImages): Boolean {
        return try {
            val data = hashMapOf(
                "phoneDocId" to phoneImages.phoneDocId,
                "manufacturer" to phoneImages.manufacturer,
                "model" to phoneImages.model,
                "colors" to phoneImages.colors.mapValues { (_, colorImages) ->
                    hashMapOf(
                        "highRes" to colorImages.highRes,
                        "lowRes" to colorImages.lowRes
                    )
                }
            )

            firestore.collection(COLLECTION_NAME)
                .document(phoneImages.phoneDocId)
                .set(data)
                .await()

            Log.d(TAG, "Successfully saved phone_images for: ${phoneImages.phoneDocId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving phone images", e)
            false
        }
    }

    /**
     * Get direct download URL from Firebase Storage path
     * Useful if you only stored the path and need the full URL
     */
    suspend fun getDownloadUrl(storagePath: String): String? {
        return try {
            val storageRef = storage.reference.child(storagePath)
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download URL for: $storagePath", e)
            null
        }
    }

    /**
     * Parse Firestore document data into PhoneImages object
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsePhoneImagesDocument(docId: String, data: Map<String, Any>?): PhoneImages? {
        if (data == null) return null

        return try {
            val colorsMap = mutableMapOf<String, ColorImages>()

            val colorsData = data["colors"] as? Map<String, Map<String, String>>
            colorsData?.forEach { (colorName, imageUrls) ->
                colorsMap[colorName] = ColorImages(
                    highRes = imageUrls["highRes"] ?: "",
                    lowRes = imageUrls["lowRes"] ?: ""
                )
            }

            PhoneImages(
                phoneDocId = data["phoneDocId"] as? String ?: docId,
                manufacturer = data["manufacturer"] as? String ?: "",
                model = data["model"] as? String ?: "",
                colors = colorsMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing phone images document: $docId", e)
            null
        }
    }
}