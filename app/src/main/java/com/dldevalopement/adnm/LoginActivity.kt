package com.dldevalopement.adnm

// Import necessary Android, Volley, Firebase, and local classes
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.ActivityLoginBinding
import com.dldevalopement.adnm.home.CollectorActivity
import com.dldevalopement.adnm.home.ReporterActivity
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

/**
 * An Activity for handling user login.
 * It provides a UI for entering credentials, handles network requests,
 * and manages user session data upon successful login.
 */
class LoginActivity : AppCompatActivity() {

    // View binding instance for safe access to views
    private lateinit var _binding: ActivityLoginBinding
    private val binding get() = _binding

    // SharedPreferences to store user session data
    private lateinit var sharedPreferences: SharedPreferences

    // A dialog to show progress during network operations
    private lateinit var progressDialog: ProgressDialog


    /**
     * Called when the activity is first created.
     * Initializes the UI and listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the progress dialog
        progressDialog = ProgressDialog(this, R.style.dialog)

        // Get an instance of SharedPreferences for application settings
        sharedPreferences = getSharedPreferences(SETTINGS, MODE_PRIVATE)

        // Set up the click listener for the login button
        binding.loginButton.setOnClickListener {
            loginUser()
        }

        // Set up the click listener for the register button
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    /**
     * Attempts to log in the user by sending a network request to the server.
     */
    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        // Validate user input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage(getString(R.string.logging_in))
        progressDialog.show()

        // Create a Volley POST request to the login endpoint
        val request = object : StringRequest(
            Method.POST, LOGIN_URL,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success) {
                        if (jsonObject.has(TOKEN)) {
                            val token = jsonObject.getString(TOKEN)
                            val role = jsonObject.getInt(ROLE)

                            // Get the FCM token from Firebase
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                val fcmToken = if (task.isSuccessful) task.result else null
                                
                                if (!task.isSuccessful) {
                                    Log.w("FCM", getString(R.string.fcm_token_failed), task.exception)
                                }

                                // Manage FCM topic subscription based on user role
                                if (role == 1) { // Collector role
                                    FirebaseMessaging.getInstance().subscribeToTopic("collectors")
                                        .addOnCompleteListener {
                                            progressDialog.dismiss()
                                            loadPage(token, role)
                                        }
                                } else { // Reporter or other roles
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic("collectors")
                                        .addOnCompleteListener {
                                            if (fcmToken != null) {
                                                sendTokenToServer(fcmToken, token, role)
                                            } else {
                                                progressDialog.dismiss()
                                                loadPage(token, role)
                                            }
                                        }
                                }
                            }
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        val message = getString(R.string.invalid_credentials)
                        AlertDialog.Builder(this, R.style.dialog)
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                } catch (e: Exception) {
                    Log.e("LOGIN", "Parsing error", e)
                    progressDialog.dismiss()
                    Toast.makeText(this, getString(R.string.error_parsing), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                error.printStackTrace()
                Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
            }
        ) {
            // Add the email and password parameters to the request body
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params[EMAIL] = email
                params[PASSWORD] = password
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Sends the FCM token to the server to associate it with the logged-in user.
     * @param fcmToken The Firebase Cloud Messaging token.
     * @param token The user's authentication token.
     * @param role The user's role.
     */
    private fun sendTokenToServer(fcmToken: String, token: String, role: Int) {
        val request = object : StringRequest(
            Method.POST, FCM_TOKEN_URL,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success) {
                        Log.d("FCM", getString(R.string.token_sent, response))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                loadPage(token, role)
            },
            { error ->
                progressDialog.dismiss()
                Log.e("FCM", getString(R.string.error_sending_token, error.message ?: ""))
                // Proceed to load the page even if sending the FCM token fails
                loadPage(token, role)
            }
        ) {
            // Add the FCM token as a parameter
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["fcm_token"] = fcmToken // Use explicit key to avoid conflict with auth TOKEN constant
                return params
            }
            // Add the authorization header with the user's token
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers[ACCEPT] = APPLICATION_JSON
                headers[AUTHORIZATION] = "$BEARER $token"
                return headers
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Navigates the user to the appropriate activity based on their role and finishes the current activity.
     * @param token The user's authentication token.
     * @param role The user's role.
     */
    private fun loadPage(token: String, role: Int) {
        // Store the token and role in SharedPreferences
        val sharedPrefs = getSharedPreferences(DATA, MODE_PRIVATE).edit()
        sharedPrefs.putString(TOKEN, token)
        sharedPrefs.putInt(ROLE, role)
        sharedPrefs.apply()

        // Redirect to the correct activity based on the role
        when (role) {
            2 -> startActivity(Intent(this, ReporterActivity::class.java)) // Reporter
            1 -> startActivity(Intent(this, CollectorActivity::class.java)) // Collector
            else -> Toast.makeText(this, getString(R.string.role_not_found), Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
