package com.techcity.techcityassist

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages local persistence of synced data.
 * Data is stored in app's internal storage and survives app restarts.
 */
object SyncDataManager {

    private const val TAG = "SyncDataManager"
    private const val PREFS_NAME = "sync_prefs"
    private const val KEY_LAST_SYNC_DATE = "last_sync_date"
    private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    private const val DEVICES_FILE = "synced_devices.json"
    private const val IMAGES_FILE = "synced_images.json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if we have valid local data from today
     */
    fun hasTodaySync(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastSyncDate = prefs.getString(KEY_LAST_SYNC_DATE, "") ?: ""
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (lastSyncDate != todayDate) {
            Log.d(TAG, "No sync today. Last sync: $lastSyncDate, Today: $todayDate")
            return false
        }

        // Also check if the data files exist
        val devicesFile = File(context.filesDir, DEVICES_FILE)
        val imagesFile = File(context.filesDir, IMAGES_FILE)

        val hasFiles = devicesFile.exists() && imagesFile.exists()
        Log.d(TAG, "Sync today: $lastSyncDate, Files exist: $hasFiles")

        return hasFiles
    }

    /**
     * Get the last sync timestamp for display purposes
     */
    fun getLastSyncTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    /**
     * Get formatted time since last sync
     */
    fun getTimeSinceSync(context: Context): String {
        val timestamp = getLastSyncTimestamp(context)
        if (timestamp == 0L) return "Never synced"

        val elapsed = System.currentTimeMillis() - timestamp
        val minutes = elapsed / 60000
        val hours = minutes / 60

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "${hours / 24} days ago"
        }
    }

    /**
     * Save synced data to local storage
     */
    fun saveSyncedData(
        context: Context,
        devices: List<Phone>,
        images: Map<String, PhoneImages>
    ): Boolean {
        return try {
            // Save devices
            val devicesJson = JSONArray()
            devices.forEach { phone ->
                devicesJson.put(phoneToJson(phone))
            }
            File(context.filesDir, DEVICES_FILE).writeText(devicesJson.toString())

            // Save images
            val imagesJson = JSONObject()
            images.forEach { (docId, phoneImages) ->
                imagesJson.put(docId, phoneImagesToJson(phoneImages))
            }
            File(context.filesDir, IMAGES_FILE).writeText(imagesJson.toString())

            // Update sync timestamp
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            getPrefs(context).edit()
                .putString(KEY_LAST_SYNC_DATE, todayDate)
                .putLong(KEY_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                .apply()

            Log.d(TAG, "Saved ${devices.size} devices and ${images.size} image sets")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving synced data", e)
            false
        }
    }

    /**
     * Load synced data from local storage
     */
    fun loadSyncedData(context: Context): Pair<List<Phone>, Map<String, PhoneImages>>? {
        return try {
            val devicesFile = File(context.filesDir, DEVICES_FILE)
            val imagesFile = File(context.filesDir, IMAGES_FILE)

            if (!devicesFile.exists() || !imagesFile.exists()) {
                Log.d(TAG, "Data files not found")
                return null
            }

            // Load devices
            val devicesJson = JSONArray(devicesFile.readText())
            val devices = mutableListOf<Phone>()
            for (i in 0 until devicesJson.length()) {
                devices.add(jsonToPhone(devicesJson.getJSONObject(i)))
            }

            // Load images
            val imagesJson = JSONObject(imagesFile.readText())
            val images = mutableMapOf<String, PhoneImages>()
            imagesJson.keys().forEach { docId ->
                images[docId] = jsonToPhoneImages(imagesJson.getJSONObject(docId))
            }

            Log.d(TAG, "Loaded ${devices.size} devices and ${images.size} image sets")
            Pair(devices, images)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading synced data", e)
            null
        }
    }

    /**
     * Clear all locally stored data (useful for forcing fresh sync)
     */
    fun clearLocalData(context: Context) {
        try {
            File(context.filesDir, DEVICES_FILE).delete()
            File(context.filesDir, IMAGES_FILE).delete()
            getPrefs(context).edit().clear().apply()
            Log.d(TAG, "Local data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local data", e)
        }
    }

    // ============================================
    // JSON Serialization Helpers
    // ============================================

    private fun phoneToJson(phone: Phone): JSONObject {
        return JSONObject().apply {
            put("manufacturer", phone.manufacturer)
            put("model", phone.model)
            put("ram", phone.ram)
            put("storage", phone.storage)
            put("retailPrice", phone.retailPrice)
            put("colors", JSONArray(phone.colors))
            put("stockCount", phone.stockCount)
            put("chipset", phone.chipset)
            put("frontCamera", phone.frontCamera)
            put("rearCamera", phone.rearCamera)
            put("batteryCapacity", phone.batteryCapacity)
            put("displayType", phone.displayType)
            put("displaySize", phone.displaySize)
            put("os", phone.os)
            put("network", phone.network)
            put("resolution", phone.resolution)
            put("refreshRate", phone.refreshRate)
            put("wiredCharging", phone.wiredCharging)
            put("inventoryDocIds", JSONArray(phone.inventoryDocIds))
            put("phoneDocId", phone.phoneDocId)
            put("deviceType", phone.deviceType)
            put("gpu", phone.gpu)
            put("cpu", phone.cpu)
        }
    }

    private fun jsonToPhone(json: JSONObject): Phone {
        return Phone(
            manufacturer = json.optString("manufacturer", ""),
            model = json.optString("model", ""),
            ram = json.optString("ram", ""),
            storage = json.optString("storage", ""),
            retailPrice = json.optDouble("retailPrice", 0.0),
            colors = jsonArrayToStringList(json.optJSONArray("colors")),
            stockCount = json.optInt("stockCount", 0),
            chipset = json.optString("chipset", ""),
            frontCamera = json.optString("frontCamera", ""),
            rearCamera = json.optString("rearCamera", ""),
            batteryCapacity = json.optInt("batteryCapacity", 0),
            displayType = json.optString("displayType", ""),
            displaySize = json.optString("displaySize", ""),
            os = json.optString("os", ""),
            network = json.optString("network", ""),
            resolution = json.optString("resolution", ""),
            refreshRate = json.optInt("refreshRate", 0),
            wiredCharging = json.optInt("wiredCharging", 0),
            inventoryDocIds = jsonArrayToStringList(json.optJSONArray("inventoryDocIds")),
            phoneDocId = json.optString("phoneDocId", ""),
            deviceType = json.optString("deviceType", ""),
            gpu = json.optString("gpu", ""),
            cpu = json.optString("cpu", ""),
            variants = emptyList()  // Variants are fetched on-demand in detail view
        )
    }

    private fun phoneImagesToJson(phoneImages: PhoneImages): JSONObject {
        return JSONObject().apply {
            put("phoneDocId", phoneImages.phoneDocId)
            put("manufacturer", phoneImages.manufacturer)
            put("model", phoneImages.model)

            val colorsJson = JSONObject()
            phoneImages.colors.forEach { (colorName, colorImages) ->
                colorsJson.put(colorName, JSONObject().apply {
                    put("highRes", colorImages.highRes)
                    put("lowRes", colorImages.lowRes)
                    put("hexColor", colorImages.hexColor)
                })
            }
            put("colors", colorsJson)
        }
    }

    private fun jsonToPhoneImages(json: JSONObject): PhoneImages {
        val colorsMap = mutableMapOf<String, ColorImages>()
        val colorsJson = json.optJSONObject("colors")

        colorsJson?.keys()?.forEach { colorName ->
            val colorJson = colorsJson.getJSONObject(colorName)
            colorsMap[colorName] = ColorImages(
                highRes = colorJson.optString("highRes", ""),
                lowRes = colorJson.optString("lowRes", ""),
                hexColor = colorJson.optString("hexColor", "")
            )
        }

        return PhoneImages(
            phoneDocId = json.optString("phoneDocId", ""),
            manufacturer = json.optString("manufacturer", ""),
            model = json.optString("model", ""),
            colors = colorsMap
        )
    }

    private fun jsonArrayToStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }
}