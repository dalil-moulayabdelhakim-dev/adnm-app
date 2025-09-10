package com.dldevalopement.adnm

// Import necessary Android classes
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * A utility object for handling runtime permission requests.
 * This object provides a static method to request a set of permissions,
 * abstracting away the version-specific logic for permissions like POST_NOTIFICATIONS.
 */
object PermissionHelper {

    // Constant for the permission request code
    const val REQUEST_NOTIFICATION_PERMISSION = 1001

    /**
     * Checks for and requests both notification and fine location permissions.
     * It handles the different requirements for Android 13 (Tiramisu) and above
     * versus older Android versions.
     *
     * @param activity The Activity context from which to request the permissions.
     */
    fun requestNotificationAndLocation(activity: Activity) {
        // Check if the device is running Android 13 (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if either POST_NOTIFICATIONS or ACCESS_FINE_LOCATION permission is not granted
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request both permissions from the user
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        } else {
            // For Android versions below 13, POST_NOTIFICATIONS permission is not required
            // Check if ACCESS_FINE_LOCATION permission is not granted
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request only the ACCESS_FINE_LOCATION permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }
}