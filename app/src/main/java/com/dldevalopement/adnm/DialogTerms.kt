package com.dldevalopement.adnm

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.database.ACCEPT
import com.dldevalopement.adnm.database.APPLICATION_JSON
import com.dldevalopement.adnm.database.EMAIL
import com.dldevalopement.adnm.database.LAST_NAME
import com.dldevalopement.adnm.database.MESSAGE
import com.dldevalopement.adnm.database.NAME
import com.dldevalopement.adnm.database.PASSWORD
import com.dldevalopement.adnm.database.PHONE_NUMBER
import com.dldevalopement.adnm.database.REGISTER_URL
import com.dldevalopement.adnm.database.TERMS_URL
import com.dldevalopement.adnm.database.USER_TYPE_ID
import com.dldevalopement.adnm.databinding.DialogTermsBinding
import org.json.JSONObject

class DialogTerms(
    private val context: Context,
    private val name : String,
    private val lastName : String,
    private val phoneNumber : String,
    private val email : String,
    private val password : String,
    private val roleId : String
    ) : DialogFragment() {
    private lateinit var _binding : DialogTermsBinding
    private val binding get() = _binding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTermsBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(context, R.style.dialog)
        builder.setTitle(context.getString(R.string.terms_and_conditions))
        builder.setCancelable(false)
        builder.setView(binding.root)
        setListeners()

        return builder.create()
    }

    private fun setListeners(){
        binding.cbAgree.setOnCheckedChangeListener { _, b ->
            binding.registerButton.isEnabled = b
        }


        binding.registerButton.setOnClickListener{
            registerUser()
        }

        binding.cancelButton.setOnClickListener{
            dismiss()
        }

        binding.tvTermsLink.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(TERMS_URL)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        // Show a progress dialog
        val progressDialog = ProgressDialog(context, R.style.dialog)
        progressDialog.setMessage(getString(R.string.please_wait))
        progressDialog.show()



        // Create a Volley POST request to the registration endpoint
        val request = object : StringRequest(
            Method.POST, REGISTER_URL,
            { response ->
                try {
                    // On successful server response
                    progressDialog.dismiss()
                    val jsonObject = JSONObject(response)
                    val message = jsonObject.getString(MESSAGE)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    // Navigate to the LoginActivity after successful registration
                    startActivity(Intent(context, LoginActivity::class.java))
                    (context as Activity).finish() // Close the current activity
                } catch (e: Exception) {
                    // Handle JSON parsing errors
                    e.printStackTrace()
                    Toast.makeText(context, getString(R.string.parsing_error, e.message), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Handle network or server-side errors
                error.printStackTrace()

            }
        ) {
            // Add the user data as parameters to the request body
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params[NAME] = name
                params[LAST_NAME] = lastName
                params[EMAIL] = email
                params[PHONE_NUMBER] = phoneNumber
                params[PASSWORD] = password
                params[USER_TYPE_ID] = roleId
                return params
            }

            // Add the necessary headers to the request
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers[ACCEPT] = APPLICATION_JSON
                return headers
            }
        }

        // Add the request to the Volley request queue
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

}