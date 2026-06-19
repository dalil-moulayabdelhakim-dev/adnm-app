package com.dldevalopement.adnm

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.ActivityProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.deleteAccountButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        binding.profileLoading.visibility = View.VISIBLE

        val request = object : StringRequest(
            Method.GET, PROFILE_URL,
            { response ->
                binding.profileLoading.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean(SUCCESS)) {
                        val userObj = jsonObject.getJSONObject("user")
                        val name = userObj.getString(NAME)
                        val lastName = userObj.getString(LAST_NAME)
                        val email = userObj.getString(EMAIL)
                        val phone = userObj.getString(PHONE_NUMBER)
                        val userTypeObj = userObj.getJSONObject("user_type")
                        val role = userTypeObj.getString("type")

                        val fullName = "$name $lastName"
                        binding.profileName.text = fullName
                        binding.profileNameHeader.text = fullName
                        binding.profileEmail.text = email
                        binding.profilePhone.text = phone
                        binding.profileRoleBadge.text = role
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing error: ${e.message}")
                    Toast.makeText(this, getString(R.string.error_parsing), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                binding.profileLoading.visibility = View.GONE
                Log.e(TAG, "Volley error: ${error.message}")
                if (error.networkResponse?.statusCode == 401) {
                    handleUnauthorized()
                } else {
                    Toast.makeText(this, getString(R.string.fetch_error, error.message), Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
                val token = sharedPreferences.getString(TOKEN, "")
                headers[ACCEPT] = APPLICATION_JSON
                headers[AUTHORIZATION] = "$BEARER $token"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.dialog)
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(R.string.no, null)
            .create()

        dialog.setOnShowListener {
            val greenColor = ContextCompat.getColor(this, R.color.green)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(greenColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(greenColor)
        }

        dialog.show()
    }

    private fun deleteAccount() {
        val progressDialog = ProgressDialog(this, R.style.dialog)
        progressDialog.setMessage(getString(R.string.deleting_account))
        progressDialog.setCancelable(false)
        progressDialog.show()

        val request = object : StringRequest(
            Method.DELETE, DELETE_ACCOUNT_URL,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean(SUCCESS)) {
                        Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_LONG).show()
                        clearSessionAndLogout()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing error: ${e.message}")
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e(TAG, "Delete error: ${error.message}")
                if (error.networkResponse?.statusCode == 401) {
                    handleUnauthorized()
                } else {
                    Toast.makeText(this, getString(R.string.error_message, error.message), Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
                val token = sharedPreferences.getString(TOKEN, "")
                headers[ACCEPT] = APPLICATION_JSON
                headers[AUTHORIZATION] = "$BEARER $token"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun clearSessionAndLogout() {
        val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE).edit()
        sharedPreferences.clear()
        sharedPreferences.apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun handleUnauthorized() {
        Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
        clearSessionAndLogout()
    }
}
