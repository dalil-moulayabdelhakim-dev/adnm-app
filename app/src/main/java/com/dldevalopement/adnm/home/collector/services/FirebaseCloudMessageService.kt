package com.dldevalopement.adnm.home.collector.services

// Import necessary Android, Firebase, and local classes
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.home.CollectorActivity
import com.dldevalopement.adnm.home.ReporterActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * A Firebase Cloud Messaging Service to handle push notifications received from Firebase.
 * This service receives messages, parses their data, and displays appropriate notifications
 * to the user based on the message content.
 */
class FirebaseCloudMessageService : FirebaseMessagingService() {

    /**
     * This method is called when a new FCM message is received.
     * It processes the message data to determine the type of notification to display.
     * @param remoteMessage The incoming RemoteMessage object containing notification and data payloads.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received: ${remoteMessage.data}")

        // Extract title and body from the notification or data payload, with default fallbacks
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.default_report_title)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: getString(R.string.default_report_body)

        // Extract specific data fields for routing and logging
        val reportId = remoteMessage.data["report_id"] ?: ""
        val status = remoteMessage.data["status"] ?: ""
        val type = remoteMessage.data["type"] ?: ""

        // Handle the notification based on the message type
        when (type) {
            "collector" -> {
                // If the message is for a collector, format the message and show a notification
                val collectorMessage = "$body\n" + getString(R.string.collector_message, reportId)
                showNotification(
                    getString(R.string.collector_title),
                    collectorMessage,
                    CollectorActivity::class.java
                )
                Log.d("FCM", "Type: $type | Report ID: $reportId | Status: $status")
            }
            "user" -> {
                // If the message is for a regular user, format the message and show a notification
                val userMessage = getString(R.string.user_message, status)
                showNotification(
                    getString(R.string.user_title),
                    userMessage,
                    ReporterActivity::class.java
                )
                Log.d("FCM_Token", "Type: $type | Report ID: $reportId | Status: $status")
            }
            else -> {
                // Log an invalid type for debugging purposes
                Log.d("FCM_Token", "Invalid user type received")
            }
        }
    }

    /**
     * Creates and displays a push notification.
     * @param title The title of the notification.
     * @param message The body text of the notification.
     * @param target The target activity to open when the notification is tapped.
     */
    private fun showNotification(title: String, message: String, target: Class<*>) {
        val channelId = "Reports"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define a custom sound for the notification
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.notification}")

            val channel = NotificationChannel(
                channelId,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                // Configure the sound and its attributes for the channel
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
            }
            // Create the channel with the notification manager
            notificationManager.createNotificationChannel(channel)
        }

        // Create an Intent to launch the target activity when the notification is tapped
        val intent = Intent(this, target)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            // Use FLAG_IMMUTABLE for security and FLAG_UPDATE_CURRENT to reuse the Intent
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification using NotificationCompat.Builder for backward compatibility
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.adnm_logo) // Set the small icon for the notification
            .setAutoCancel(true) // Dismiss the notification when the user taps on it
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the notification priority
            .setContentIntent(pendingIntent) // Set the intent to be launched
            .build()

        // Display the notification with a unique ID based on the current time
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}