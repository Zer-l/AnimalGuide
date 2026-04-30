package com.permissionx.animalguide.ui.privacy

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)

    val hasAgreed: Boolean
        get() = prefs.getBoolean("agreed", false)

    fun setAgreed() {
        prefs.edit().putBoolean("agreed", true).apply()
    }
}
