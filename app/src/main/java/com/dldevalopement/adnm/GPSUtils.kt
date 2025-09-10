package com.dldevalopement.adnm

// Import necessary Android classes
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE

/**
 * A utility class for GPS-related operations.
 * It uses a `companion object` to provide static methods that can be called without
 * creating an instance of the class.
 */
class GPSUtils {

    companion object {
        /**
         * Checks if the device's GPS provider is enabled.
         *
         * @param context The application context.
         * @return `true` if GPS is enabled, `false` otherwise.
         */
        fun isGPSEnabled(context: Context): Boolean {
            // Get the LocationManager service
            val locationManager = context.getSystemService(LOCATION_SERVICE) as android.location.LocationManager
            // Check if the GPS provider is enabled
            return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        }

        /**
         * Displays an alert dialog that prompts the user to enable GPS.
         * If the user clicks "Enable", it opens the device's location settings.
         *
         * @param context The application context.
         */
        fun showGPSDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.enable_gps_title))
                .setMessage(context.getString(R.string.enable_gps_message))
                .setPositiveButton(context.getString(R.string.enable)) { _, _ ->
                    // Create an intent to open the location source settings
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    // Start the activity, ensuring the context is an Activity
                    (context as? Activity)?.startActivity(intent)
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    // Dismiss the dialog if the user clicks "Cancel"
                    dialog.dismiss()
                }
                .show() // Show the dialog to the user
        }
    }
}