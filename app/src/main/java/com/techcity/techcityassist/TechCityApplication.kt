package com.techcity.techcityassist

import android.app.Application

class TechCityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Revocation detector: re-validates the signed-in account on every
        // activity resume and on a periodic tick while foregrounded.
        Authmanager.startSessionGuard(this)
        // Restore the persisted store location before any screen reads it.
        LocationManager.init(this)
    }
}
