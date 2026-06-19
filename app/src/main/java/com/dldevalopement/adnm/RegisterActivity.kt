package com.dldevalopement.adnm

// Import necessary Android, Volley, and local classes
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.ActivityRegisterActivityBinding
import org.json.JSONObject

/**
 * An Activity for handling user registration.
 * It provides a UI for new users to create an account by entering their details
 * and selecting a role (Collector or Reporter).
 */
class RegisterActivity : AppCompatActivity() {

    // View binding instance for safe access to views
    private lateinit var _binding: ActivityRegisterActivityBinding
    private val binding get() = _binding

    private var isVerified = false

    /**
     * Called when the activity is first created.
     * Initializes the UI, sets up the spinner, and configures click listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRegisterActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.recaptchaButton.setOnClickListener {
            verify(this, binding.recaptchaProgressbar, binding.recaptchaCheckbox, binding.errorIc)
        }

        binding.recaptchaCheckbox.setOnClickListener {
            verify(this, binding.recaptchaProgressbar, binding.recaptchaCheckbox, binding.errorIc)
        }
        // Set click listeners for the registration and back buttons
        binding.registerButton.setOnClickListener {
            prepare()
        }

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.cancelButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * Handles the user registration process.
     * It collects user data, validates the input, and sends a POST request to the server.
     */

    private fun prepare(){
        // Get user input from the EditText fields
        val name = binding.nameEditText.text.toString().trim()
        val lastName = binding.lastNameEditText.text.toString().trim()
        val phoneNumber = binding.phoneEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // Validate that the passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            return
        }

        if (!isVerified){
            Toast.makeText(this, getString(R.string.check_the_box_message), Toast.LENGTH_LONG).show()
            return
        }

        val dialog = DialogTerms(this, name, lastName, phoneNumber, email, password, "2")  // 2 for Reporter
        dialog.show(supportFragmentManager, "terms")
    }

    /**
     * Verifies the user through Google reCAPTCHA by sending a POST request to the backend.
     *
     * This function updates the UI based on the verification status — showing a progress bar
     * while the verification is in progress, and updating the checkbox or error icon depending
     * on the result.
     *
     * @param context The current context used for UI operations and accessing resources.
     * @param progressBar The ProgressBar displayed during verification.
     * @param checkBox The CheckBox that indicates whether verification was successful.
     * @param errorIC The ImageView used to display an error icon if the verification fails.
     */
    private fun verify(context: Context, progressBar: ProgressBar, checkBox: CheckBox, errorIC: ImageView) {
        // Show loading indicator and hide checkbox while verifying
        progressBar.visibility = View.VISIBLE
        checkBox.visibility = View.GONE

        // Create a new Volley request queue
        val queue = Volley.newRequestQueue(context)

        // Create a POST request to verify the reCAPTCHA token
        val request = object : StringRequest(
            Method.POST, RECAPTCHA_URL,
            Response.Listener { res ->
                // Parse the JSON response
                val jsonObject = JSONObject(res)
                val success = jsonObject.getBoolean(SUCCESS)
                isVerified = success

                if (success) {
                    // ✅ Verification succeeded: show checkbox and disable further verification
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    checkBox.isEnabled = false
                    binding.recaptchaButton.isEnabled = false
                } else {
                    // ❌ Verification failed: show error message
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    Toast.makeText(context, jsonObject.getString(MESSAGE), Toast.LENGTH_SHORT).show()
                }

                // Reflect verification status in checkbox state
                checkBox.isChecked = success
            },
            Response.ErrorListener { error ->
                // ❌ Network or server error occurred
                error.printStackTrace()
                errorIC.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                checkBox.visibility = View.GONE
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            /**
             * Sends the reCAPTCHA token to the backend for validation.
             *
             * @return A map containing the token parameter.
             */
            override fun getParams(): Map<String, String> {
                val safeToken = context.getString(R.string.recaptcha_site_key)
                return mapOf(TOKEN to safeToken)
            }
        }

        // Add the request to the queue for execution
        queue.add(request)
    }




}