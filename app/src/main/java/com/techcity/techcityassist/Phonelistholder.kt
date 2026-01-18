package com.techcity.techcityassist

/**
 * Simple holder to pass the filtered phone list between MainActivity and PhoneDetailActivity.
 * This avoids Intent size limits when passing large lists.
 */
object PhoneListHolder {
    var filteredPhones: List<Phone> = emptyList()
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
}