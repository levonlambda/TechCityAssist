package com.techcity.techcityassist

/**
 * Singleton holder for caching all device data.
 * Data is loaded once via SYNC and reused across category navigation.
 */
object PhoneListHolder {
    // ============================================
    // CACHED DATA (populated by SYNC)
    // ============================================

    /** All devices from inventory (phones, tablets, laptops) */
    var allDevices: List<Phone> = emptyList()
        private set

    /** All phone images mapped by phoneDocId */
    var allPhoneImages: Map<String, PhoneImages> = emptyMap()
        private set

    /** Whether data has been synced at least once */
    var isSynced: Boolean = false
        private set

    /** Timestamp of last successful sync */
    var lastSyncTime: Long = 0L
        private set

    // ============================================
    // FILTERED DATA (for current view)
    // ============================================

    /** Currently filtered phones for PhoneDetailActivity navigation */
    var filteredPhones: List<Phone> = emptyList()

    /** Phone images for current filtered list */
    var phoneImagesMap: Map<String, PhoneImages> = emptyMap()

    /**
     * Returns a list of unique phone models (one per manufacturer+model combination).
     * Uses the first variant (lowest price) as the representative for each model.
     */
    val uniquePhoneModels: List<Phone>
        get() = filteredPhones
            .groupBy { "${it.manufacturer}|${it.model}" }
            .map { (_, variants) -> variants.first() }

    /**
     * Get the index in uniquePhoneModels for a given phone
     */
    fun getUniqueModelIndex(phone: Phone): Int {
        val key = "${phone.manufacturer}|${phone.model}"
        return uniquePhoneModels.indexOfFirst { "${it.manufacturer}|${it.model}" == key }
            .coerceAtLeast(0)
    }

    // ============================================
    // SYNC METHODS
    // ============================================

    /**
     * Store synced data from Firebase
     */
    fun setSyncedData(devices: List<Phone>, images: Map<String, PhoneImages>) {
        allDevices = devices
        allPhoneImages = images
        isSynced = true
        lastSyncTime = System.currentTimeMillis()
    }

    /**
     * Get devices filtered by type (phone, tablet, laptop)
     */
    fun getDevicesByType(deviceType: String): List<Phone> {
        return allDevices.filter { it.deviceType.equals(deviceType, ignoreCase = true) }
    }

    /**
     * Get images for a list of devices
     */
    fun getImagesForDevices(devices: List<Phone>): Map<String, PhoneImages> {
        val docIds = devices.mapNotNull { it.phoneDocId.ifEmpty { null } }.distinct()
        return allPhoneImages.filterKeys { it in docIds }
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        allDevices = emptyList()
        allPhoneImages = emptyMap()
        isSynced = false
        lastSyncTime = 0L
        filteredPhones = emptyList()
        phoneImagesMap = emptyMap()
    }

    /**
     * Get formatted time since last sync
     */
    fun getTimeSinceSync(): String {
        if (lastSyncTime == 0L) return "Never synced"

        val elapsed = System.currentTimeMillis() - lastSyncTime
        val minutes = elapsed / 60000
        val hours = minutes / 60

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "${hours / 24} days ago"
        }
    }
}