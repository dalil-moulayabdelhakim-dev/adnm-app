package com.dldevalopement.adnm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.database.CONFIG_URL
import com.dldevalopement.adnm.database.DATA
import com.dldevalopement.adnm.database.ROLE
import com.dldevalopement.adnm.database.TOKEN
import com.dldevalopement.adnm.databinding.ActivitySplashScreenBinding
import com.dldevalopement.adnm.home.CollectorActivity
import com.dldevalopement.adnm.home.ReporterActivity

class SplashScreen : AppCompatActivity() {

    private lateinit var _binding: ActivitySplashScreenBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(_binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.version.text = "${BuildConfig.VERSION_NAME} (V${BuildConfig.VERSION_CODE})"

        // الخطوة 1: التحقق من التحديث عبر السيرفر الخاص بك أولاً
        checkServerForUpdate()
    }

    private fun checkServerForUpdate() {
        val request = JsonObjectRequest(Request.Method.GET, CONFIG_URL, null,
            { response ->
                val minVersionCode = response.optInt("min_version_code", 0)
                val isRequired = response.optBoolean("is_update_required", false)
                val playStoreUrl = response.optString("play_store_url")
                val serverUrl = response.optString("server_download_url")

                // استخدام النص من السيرفر أو النص الافتراضي من الموارد
                val message = response.optString("message", getString(R.string.default_update_message))

                val currentVersionCode = BuildConfig.VERSION_CODE

                if (currentVersionCode < minVersionCode) {
                    showUpdateDialog(playStoreUrl, serverUrl, message, isRequired)
                } else {
                    loadPage()
                }
            },
            { error ->
                loadPage()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun showUpdateDialog(playUrl: String, serverUrl: String, message: String, isRequired: Boolean) {
        val title = if (isRequired) getString(R.string.update_required_title) else getString(R.string.update_available_title)

        val builder = AlertDialog.Builder(this, R.style.dialog)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(!isRequired)
            .setPositiveButton(getString(R.string.update_now)) { _, _ ->
                handleUpdateAction(playUrl, serverUrl)
            }

        if (isRequired) {
            builder.setNegativeButton(getString(R.string.exit_app)) { _, _ ->
                finish()
            }
        } else {
            builder.setNegativeButton(getString(R.string.later)) { _, _ ->
                loadPage()
            }
        }

        builder.show()
    }

    private fun handleUpdateAction(playUrl: String, serverUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playUrl))
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // استخدام نصوص التوست من الموارد
            Toast.makeText(this, getString(R.string.play_store_not_found), Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.update_failed_app), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadPage() {
        val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
        val token = sharedPreferences.getString(TOKEN, null)
        val role = sharedPreferences.getInt(ROLE, 0)

        if (token != null) {
            val nextIntent = when (role) {
                2 -> Intent(this, ReporterActivity::class.java)
                1 -> Intent(this, CollectorActivity::class.java)
                else -> null
            }

            if (nextIntent != null) {
                startActivity(nextIntent)
                finish()
            } else {
                Toast.makeText(this, getString(R.string.role_not_found), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}