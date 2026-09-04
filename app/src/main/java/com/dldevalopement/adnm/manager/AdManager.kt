package com.dldevalopement.adnm.manager

import android.content.Context
import android.util.Log
import android.view.View
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.StartAppSDK

/**
 * AdManager is a utility singleton for managing Start.io SDK initialization
 * and providing standardized ad configurations.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Replace with your actual Start.io App ID
    private const val STARTIO_APP_ID = "208582499" // Placeholder Test ID

    /**
     * Initializes the Start.io SDK.
     * Call this in your Application class or the main entry Activity.
     *
     * @param context The context for initialization.
     */
    fun initialize(context: Context) {
        StartAppSDK.init(context, STARTIO_APP_ID, true)
        Log.d(TAG, "Start.io SDK initialized")
    }

    /**
     * Creates a banner ad view.
     *
     * @param context The context to create the banner.
     * @return A View containing the banner ad.
     */
    fun createBannerAd(context: Context): View {
        return Banner(context)
    }
}
