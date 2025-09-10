package com.dldevalopement.adnm.manager

// Import necessary Android, Volley, and Firebase classes
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.LoginActivity
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.ACCEPT
import com.dldevalopement.adnm.database.APPLICATION_JSON
import com.dldevalopement.adnm.database.AUTHORIZATION
import com.dldevalopement.adnm.database.BEARER
import com.dldevalopement.adnm.database.DATA
import com.dldevalopement.adnm.database.LOGOUT_URL
import com.dldevalopement.adnm.database.RECAPTCHA_URL
import com.dldevalopement.adnm.database.SUCCESS
import com.dldevalopement.adnm.database.TOKEN
import com.google.android.gms.safetynet.SafetyNet
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

/**
 * A utility class to manage authentication-related tasks.
 * It uses a companion object to provide a static `logout` function, making it accessible
 * without creating an instance of the class.
 */
class AuthManager {

    companion object {
        /**
         * Initiates the logout process.
         * It shows a confirmation dialog, sends a request to the server, and clears local data.
         *
         * @param context The context from which the function is called.
         */
        fun logout(context: Context) {
            // Build and show a confirmation dialog
            val builder = AlertDialog.Builder(context, R.style.dialog)
            val dialog = builder.setTitle(context.getString(R.string.logout_title))
                .setIcon(R.drawable.ic_logout)
                .setMessage(context.getString(R.string.logout_message))
                // Set up the "Yes" button to proceed with logout
                .setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                    dialog.dismiss()

                    // Show a progress dialog while logging out
                    val progressDialog = ProgressDialog(context, R.style.dialog)
                    progressDialog.setMessage(context.getString(R.string.logging_out))
                    progressDialog.setCancelable(false)
                    progressDialog.show()

                    // Create a Volley POST request to the logout endpoint
                    val request = object : StringRequest(
                        Method.POST, LOGOUT_URL,
                        { response ->
                            // On successful server response
                            progressDialog.dismiss()
                            Log.d("Logout", response)

                            // 1. Delete the FCM token from the device
                            FirebaseMessaging.getInstance().deleteToken()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // 2. Unsubscribe from the 'collectors' topic
                                        FirebaseMessaging.getInstance()
                                            .unsubscribeFromTopic("collectors")
                                            .addOnCompleteListener {
                                                // 3. Clear all data from SharedPreferences
                                                val sharedPreferences = context.getSharedPreferences(DATA, MODE_PRIVATE).edit()
                                                sharedPreferences.clear()
                                                sharedPreferences.apply()

                                                // 4. Redirect to the LoginActivity and clear the back stack
                                                val intent = Intent(context, LoginActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                context.startActivity(intent)
                                                (context as Activity).finish()
                                            }
                                    }
                                }
                        },
                        { error ->
                            // On error response from the server
                            progressDialog.dismiss()
                            Log.e("Logout", error.toString())
                            error.printStackTrace()
                        }
                    ) {
                        // Override to add necessary headers for the API request
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            val sharedPreferences = context.getSharedPreferences(DATA, MODE_PRIVATE)
                            headers[ACCEPT] = APPLICATION_JSON
                            headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, "")}"
                            return headers
                        }
                    }

                    // Add the request to the Volley queue
                    Volley.newRequestQueue(context).add(request)
                }
                // Set up the "No" button to cancel the logout
                .setNegativeButton(context.getString(R.string.no)) { d, _ -> d.cancel() }
                .create()

            // Customize the button colors after the dialog is shown
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.resources.getColor(R.color.green))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getColor(R.color.green))
            }

            dialog.show()
        }


    }
}