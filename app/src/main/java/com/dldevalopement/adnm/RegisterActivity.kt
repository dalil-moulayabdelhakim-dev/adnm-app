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
import androidx.appcompat.app.AppCompatActivity
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

    // Variable to store the selected user role (1 for Collector, 2 for Reporter)
    private var selectedRole: Int = 1

    private var isVerified = false

    /**
     * Called when the activity is first created.
     * Initializes the UI, sets up the spinner, and configures click listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recaptchaButton.setOnClickListener {
            verify(this, binding.recaptchaProgressbar, binding.recaptchaCheckbox, binding.errorIc)
        }

        binding.userRoleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedRole = when (checkedId) {
                R.id.reporter_radio_button -> 2
                R.id.collector_radio_button -> 1
                else -> 0
            }
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

        if (selectedRole == 0){
            Toast.makeText(this, getString(R.string.select_role_message), Toast.LENGTH_LONG).show()
            return
        }

        // Validate that the passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            return
        }

        if (!isVerified){
            Toast.makeText(this, getString(R.string.check_the_box_message), Toast.LENGTH_LONG).show()
            return
        }

        val dialog = DialogTerms(this, name, lastName, phoneNumber, email, password, selectedRole.toString())
        dialog.show(supportFragmentManager, "terms")
    }

    private fun verify(context: Context, progressBar: ProgressBar, checkBox: CheckBox, errorIC: ImageView){
        progressBar.visibility = View.VISIBLE
        checkBox.visibility = View.GONE
        val queue = Volley.newRequestQueue(context)

        val request = object : StringRequest(
            Method.POST, RECAPTCHA_URL,
            Response.Listener { res ->
                val jsonObject = JSONObject(res)
                val success = jsonObject.getBoolean(SUCCESS)
                isVerified = success
                if (success){
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    checkBox.isEnabled = false
                    binding.recaptchaButton.isEnabled = false
                }else{
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    Toast.makeText(context, jsonObject.getString(MESSAGE), Toast.LENGTH_SHORT).show()
                }
                checkBox.isChecked = success

            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                errorIC.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                checkBox.visibility = View.GONE
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val safeToken = context.getString(R.string.recaptcha_site_key)
                return mapOf(TOKEN to safeToken)
            }
        }
        queue.add(request)
    }



}