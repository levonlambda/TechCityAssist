package com.techcity.techcityassist

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Statuses that count as available for display in the app.
 * A sale flips an inventory document's status to "Sold", which removes it
 * from any query filtered on these values.
 */
val AVAILABLE_INVENTORY_STATUSES = listOf("On-Hand", "On-Display")

/**
 * Rebuild the specs map from already-grouped Phone objects — every DeviceSpecs
 * field is embedded in each Phone — so a cache hit doesn't need to re-read the
 * phones collection before attaching the live inventory listener. Models with
 * no available inventory at sync time are absent; their specs load on the next
 * cold start.
 */
fun specsMapFromDevices(devices: List<Phone>): Map<String, DeviceSpecs> {
    return devices
        .groupBy { "${it.manufacturer}|${it.model}" }
        .mapValues { (_, phones) ->
            val p = phones.first()
            DeviceSpecs(
                docId = p.phoneDocId,
                chipset = p.chipset,
                frontCamera = p.frontCamera,
                rearCamera = p.rearCamera,
                battery = p.batteryCapacity,
                os = p.os,
                network = p.network,
                display = p.displayType,
                displaySize = p.displaySize,
                resolution = p.resolution,
                refreshRate = p.refreshRate,
                wiredCharging = p.wiredCharging,
                deviceType = p.deviceType,
                gpu = p.gpu,
                cpu = p.cpu
            )
        }
}

private data class InventoryUnit(
    val manufacturer: String,
    val model: String,
    val ram: String,
    val storage: String,
    val color: String,
    val retailPrice: Double,
    val dealersPrice: Double,
    val docId: String
)

/**
 * Groups available inventory documents into Phone entries keyed by
 * manufacturer|model|ram|storage. Shared by the PhoneListActivity live
 * listener and MainActivity's sync so both produce identical results.
 */
fun groupInventoryDocs(
    docs: List<DocumentSnapshot>,
    specsMap: Map<String, DeviceSpecs>
): List<Phone> {
    return docs
        .mapNotNull { doc ->
            val manufacturer = doc.getString("manufacturer") ?: return@mapNotNull null
            val model = doc.getString("model") ?: return@mapNotNull null
            InventoryUnit(
                manufacturer = manufacturer,
                model = model,
                ram = doc.getString("ram") ?: "",
                storage = doc.getString("storage") ?: "",
                color = doc.getString("color") ?: "",
                retailPrice = doc.getDouble("retailPrice") ?: 0.0,
                dealersPrice = doc.getDouble("dealersPrice") ?: 0.0,
                docId = doc.id
            )
        }
        .groupBy { "${it.manufacturer}|${it.model}|${it.ram}|${it.storage}" }
        .map { (_, items) ->
            val first = items.first()
            val colors = items.map { it.color }.distinct().filter { it.isNotEmpty() }
            val specs = specsMap["${first.manufacturer}|${first.model}"] ?: DeviceSpecs()

            Phone(
                manufacturer = first.manufacturer,
                model = first.model,
                ram = first.ram,
                storage = first.storage,
                retailPrice = first.retailPrice,
                colors = colors,
                stockCount = items.size,
                chipset = specs.chipset,
                frontCamera = specs.frontCamera,
                rearCamera = specs.rearCamera,
                batteryCapacity = specs.battery,
                displayType = specs.display,
                displaySize = specs.displaySize,
                os = specs.os,
                network = specs.network,
                resolution = specs.resolution,
                refreshRate = specs.refreshRate,
                wiredCharging = specs.wiredCharging,
                inventoryDocIds = items.map { it.docId },
                phoneDocId = specs.docId,
                variants = emptyList(),
                deviceType = specs.deviceType,
                gpu = specs.gpu,
                cpu = specs.cpu
            )
        }
        .sortedWith(compareBy({ it.manufacturer }, { it.model }, { it.retailPrice }))
}
