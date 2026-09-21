package com.techcity.techcityassist

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Availability of a device variant (in a given colour) relative to the
 * store location the app is currently set to.
 */
enum class LocationAvailability {
    /** At least one unit is stocked at the selected location */
    HERE,
    /** Units exist, but only at other locations (or with no location recorded) */
    ELSEWHERE,
    /** No unit exists anywhere */
    NONE
}

/** Two-tone bar for the "stocked at another location only" state: blue over black */
val LOCATION_ELSEWHERE_TOP_COLOR = Color(0xFF2196F3)
val LOCATION_ELSEWHERE_BOTTOM_COLOR = Color(0xFF000000)

/**
 * Owns the store location the app is "set to": the list of locations read
 * from the accessory_locations collection, the selected one (persisted in
 * SharedPreferences), the default rule, and the availability rule shared by
 * the device list cards and the detail screen.
 *
 * selectedLocation and locations are Compose state so any composable that
 * reads them recomposes when the user picks a different location.
 */
object LocationManager {

    private const val TAG = "LocationManager"
    private const val PREFS_NAME = "location_prefs"
    private const val KEY_SELECTED_LOCATION = "selected_location"
    private const val COLLECTION = "accessory_locations"
    private const val NAME_FIELD = "name"

    /** Location the app is currently set to; blank when none could be determined */
    var selectedLocation by mutableStateOf("")
        private set

    /** All known locations, in collection order */
    var locations by mutableStateOf<List<String>>(emptyList())
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Restore the persisted selection. Called once from TechCityApplication.
     */
    fun init(context: Context) {
        selectedLocation = getPrefs(context).getString(KEY_SELECTED_LOCATION, "") ?: ""
        Log.d(TAG, "Restored location: '$selectedLocation'")
    }

    /**
     * Fetch the location list and apply the default rule:
     * - nothing selected yet -> first location in the collection
     * - selected location no longer exists -> first location in the collection
     * - fetch failed or empty -> keep whatever is selected (persisted value survives offline)
     */
    suspend fun loadLocations(context: Context) {
        val fetched = try {
            FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.getString(NAME_FIELD)?.trim()?.ifEmpty { null } }
        } catch (e: Exception) {
            if (!Authmanager.handleFirestoreError(context, e)) {
                Log.e(TAG, "Failed to load locations", e)
            }
            return
        }

        if (fetched.isEmpty()) {
            Log.w(TAG, "No locations found in $COLLECTION")
            return
        }

        locations = fetched

        val current = selectedLocation
        val stillExists = fetched.any { matches(it, current) }
        if (current.isBlank() || !stillExists) {
            select(context, fetched.first())
            Log.d(TAG, "Defaulted location to '${fetched.first()}'")
        }
    }

    /**
     * Set the current location and persist it. Touches nothing else.
     */
    fun select(context: Context, name: String) {
        selectedLocation = name
        getPrefs(context).edit().putString(KEY_SELECTED_LOCATION, name).apply()
    }

    /**
     * Label shown on the home and detail screens.
     */
    fun displayLabel(): String {
        return "Location: " + selectedLocation.ifBlank { "No location set" }
    }

    /**
     * Location name alone, upper-cased, for the home and detail screen headers.
     */
    fun displayName(): String {
        return selectedLocation.ifBlank { "No location set" }.uppercase()
    }

    /**
     * Availability of a variant in one colour, given the locations of every
     * unit of that variant in that colour (one entry per unit, blank when the
     * inventory document has no location).
     */
    fun availability(
        unitLocations: List<String>?,
        selected: String = selectedLocation
    ): LocationAvailability {
        if (unitLocations.isNullOrEmpty()) return LocationAvailability.NONE
        // No location set: never show the "elsewhere" indicator
        if (selected.isBlank()) return LocationAvailability.HERE
        return if (unitLocations.any { matches(it, selected) }) {
            LocationAvailability.HERE
        } else {
            LocationAvailability.ELSEWHERE
        }
    }

    /**
     * Look up the unit locations for a colour with case-insensitive key matching.
     */
    fun locationsForColor(
        colorLocations: Map<String, List<String>>,
        colorName: String
    ): List<String>? {
        return colorLocations.entries
            .firstOrNull { it.key.equals(colorName, ignoreCase = true) }
            ?.value
    }

    private fun matches(a: String, b: String): Boolean {
        return a.trim().equals(b.trim(), ignoreCase = true)
    }
}

/**
 * The thin vertical availability bar shown after the RAM/Storage chips on
 * the device list cards and the detail screen. One place to restyle the
 * "elsewhere" indicator later.
 */
@Composable
fun AvailabilityBar(
    state: LocationAvailability,
    fillColor: Color,
    width: Dp,
    height: Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .then(
                if (state != LocationAvailability.NONE) {
                    Modifier.border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(3.dp))
                } else {
                    Modifier
                }
            )
    ) {
        when (state) {
            LocationAvailability.HERE -> {
                Box(modifier = Modifier.fillMaxSize().background(fillColor))
            }
            LocationAvailability.ELSEWHERE -> {
                // Upper half blue, lower half black
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(LOCATION_ELSEWHERE_TOP_COLOR)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(LOCATION_ELSEWHERE_BOTTOM_COLOR)
                    )
                }
            }
            LocationAvailability.NONE -> Unit
        }
    }
}
