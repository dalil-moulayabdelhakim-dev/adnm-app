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
    fun requestPermissions(activity: Activity) {
        // قائمة الأذونات الأساسية المطلوبة لجميع الإصدارات
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )

        // إضافة إذن الإشعارات فقط إذا كان الإصدار أندرويد 13 (API 33) أو أعلى
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // فحص الأذونات: هل هناك إذن واحد على الأقل غير ممنوح؟
        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            // طلب الأذونات غير الممنوحة فقط
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }
}