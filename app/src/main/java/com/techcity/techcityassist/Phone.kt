package com.techcity.techcityassist

data class PhoneVariant(
    val ram: String = "",
    val storage: String = "",
    val retailPrice: Double = 0.0,
    val dealersPrice: Double = 0.0
)

data class Phone(
    val manufacturer: String = "",
    val model: String = "",
    val ram: String = "",
    val storage: String = "",
    val retailPrice: Double = 0.0,
    val colors: List<String> = emptyList(),
    val stockCount: Int = 0,
    val chipset: String = "",
    val frontCamera: String = "",
    val rearCamera: String = "",
    val batteryCapacity: Int = 0,
    val displayType: String = "",
    val displaySize: String = "",
    val os: String = "",
    val network: String = "",
    val resolution: String = "",
    val refreshRate: Int = 0,
    val wiredCharging: Int = 0,
    val inventoryDocIds: List<String> = emptyList(),
    val phoneDocId: String = "",
    val variants: List<PhoneVariant> = emptyList(),
    val deviceType: String = ""  // Phone, Tablet, Laptop
)