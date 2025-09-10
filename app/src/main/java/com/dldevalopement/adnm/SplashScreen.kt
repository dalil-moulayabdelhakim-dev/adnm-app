package com.dldevalopement.adnm

// Import necessary Android, local, and external libraries
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dldevalopement.adnm.database.DATA
import com.dldevalopement.adnm.database.ROLE
import com.dldevalopement.adnm.database.TOKEN
import com.dldevalopement.adnm.databinding.ActivitySplashScreenBinding
import com.dldevalopement.adnm.home.CollectorActivity
import com.dldevalopement.adnm.home.ReporterActivity
import com.dldevalopement.adnm.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task

/**
 * A SplashScreen activity that handles the initial startup logic of the application.
 * It checks for an existing user session and redirects the user accordingly.
 */
class SplashScreen : AppCompatActivity() {

    // View binding instance for safe access to views
    private lateinit var _binding: ActivitySplashScreenBinding
    private val binding get() = _binding

    private lateinit var appUpdateManager: AppUpdateManager
    private val REQUEST_CODE_UPDATE = 1234

    // A Handler to post a delayed action to navigate away from the splash screen
    private val handler = Handler()

    /**
     * Called when the activity is first created.
     * Sets up the layout, displays the app version, and schedules the redirection.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // Create an instance of AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)

// Check if there is an update available for the app
        checkForUpdate()


        // Set the version text using BuildConfig and device information
        // Note: The `BuildConfig` from 'com.google.android.datatransport' is likely a typo.
        // It should be the one generated for your app, located at the app-level package.
        binding.version.text =
            "V${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"

        // Schedule the `loadPage` function to run after a 2-second delay
        handler.postDelayed(runnable, 2000)
    }

    // A Runnable that calls the `loadPage` function
    private val runnable = Runnable {
        loadPage()
    }

    /**
     * Determines which page to load based on the user's login status and role.
     * It checks for an existing authentication token in SharedPreferences.
     */
    private fun loadPage() {
        // Retrieve the SharedPreferences instance
        val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
        // Get the authentication token and user role
        val token = sharedPreferences.getString(TOKEN, null)
        val role = sharedPreferences.getInt(ROLE, 0)

        // Check if a token exists, which indicates a logged-in user
        if (token != null) {
            // Redirect based on the user's role
            when (role) {
                2 -> startActivity(Intent(this, ReporterActivity::class.java)) // Reporter role
                1 -> startActivity(Intent(this, CollectorActivity::class.java)) // Collector role
                else -> Toast.makeText(this, getString(R.string.role_not_found), Toast.LENGTH_SHORT)
                    .show()
            }
            // Finish the splash screen activity so the user can't return to it
            finish()
        } else {
            // If no token exists, the user is not logged in, so navigate to the LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            // Finish the splash screen activity
            finish()
        }
    }

    // Function to check for app updates using Play Core In-App Updates API
    private fun checkForUpdate() {
        // Get the task that returns AppUpdateInfo
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo

        // Add a listener when the task succeeds
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            // Check if an update is available and if immediate updates are allowed
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Start the immediate update flow (full-screen dialog)
                startUpdate(appUpdateInfo)
            }else{
                loadPage()
            }
        }
    }

    // Function to start the update flow
    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,               // The update info retrieved from Play Store
                AppUpdateType.IMMEDIATE,     // Use immediate update (full-screen)
                this,                        // Activity context
                REQUEST_CODE_UPDATE          // Request code to identify the result
            )
        } catch (e: IntentSender.SendIntentException) {
            // Handle any errors that may occur while starting the update
            e.printStackTrace()
        }
    }

    // Handle the result of the update flow
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                // The user either canceled the update or an error occurred
                // Here you can close the app or show a message to the user
            }
        }
    }

}