package com.dldevalopement.adnm.home

// Import necessary Android classes and libraries
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.PermissionHelper
import com.dldevalopement.adnm.ProfileActivity
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.AUTHORIZATION
import com.dldevalopement.adnm.database.BEARER
import com.dldevalopement.adnm.database.CREATED_AT
import com.dldevalopement.adnm.database.DATA
import com.dldevalopement.adnm.database.ID
import com.dldevalopement.adnm.database.MESSAGE
import com.dldevalopement.adnm.database.REPORTER_MY_REPORTS_URL
import com.dldevalopement.adnm.database.REPORTS
import com.dldevalopement.adnm.database.STATUS
import com.dldevalopement.adnm.database.SUCCESS
import com.dldevalopement.adnm.database.TOKEN
import com.dldevalopement.adnm.database.TOTAL_PRICE
import com.dldevalopement.adnm.databinding.ActivityReporterBinding
import com.dldevalopement.adnm.home.reporter.dialog.AddReportDialogFragment
import com.dldevalopement.adnm.home.reporter.RecyclerInterface
import com.dldevalopement.adnm.home.reporter.Report
import com.dldevalopement.adnm.home.reporter.dialog.ReportInfoDialog
import com.dldevalopement.adnm.home.reporter.ReportsAdapter
import com.dldevalopement.adnm.manager.AuthManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The main activity for a user with the 'Reporter' role.
 * This activity displays a list of reports and allows the user to add new ones.
 */
class ReporterActivity : AppCompatActivity(), AddReportDialogFragment.ReportStatusListener{

    // View binding instance to access the layout's views
    private lateinit var binding: ActivityReporterBinding
    // Adapter for the RecyclerView to display the list of reports
    private lateinit var reportsAdapter: ReportsAdapter

    /**
     * Called when the activity is first created.
     * Initializes the UI and sets up event listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using view binding and set it as the content view
        binding = ActivityReporterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Request necessary permissions (e.g., notifications and location) from the user
        PermissionHelper.requestPermissions(this)

        // 🔹 RecyclerView setup
        // Initialize the reports adapter with a click listener for items
        reportsAdapter = ReportsAdapter(this, emptyList(), object : RecyclerInterface {
            override fun onItemClick(position: Int) {
                // Get the clicked report item
                val report = reportsAdapter.reports[position]
                // Create and show a dialog to display detailed information about the report
                val dialog = ReportInfoDialog(this@ReporterActivity, report.id)
                dialog.show(supportFragmentManager, "report info dialog")
            }
        })
        // Set up the RecyclerView with a linear layout manager and the custom adapter
        binding.reportsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reportsRecyclerView.adapter = reportsAdapter

        // 🔄 Swipe Refresh
        // Set up the swipe-to-refresh listener to reload reports
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchReports()
        }

        // ➕ Button to add a new report
        binding.addReportButton.setOnClickListener {
            // Create and show a dialog fragment to add a new report
            val dialog = AddReportDialogFragment(this)
            dialog.setReportStatusListener(this)
            dialog.show(supportFragmentManager, "add report dialog")
        }

        // Set up the profile button listener
        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Set up the logout button listener
        binding.logoutButton.setOnClickListener {
            AuthManager.logout(this)
        }

        // 📡 Fetch data for the first time when the activity starts
        fetchReports()
    }

    /**
     * Handles the result of a permission request.
     * @param requestCode The request code passed to requestPermissions.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the result is for the notification permission request
        if (requestCode == PermissionHelper.REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Toast.makeText(this, getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                // Permission was denied. Show a custom dialog or toast.
                // The user denied the permission but did not check "Don't ask again".
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showPermissionDeniedDialog()
                } else {
                    // The user denied the permission permanently ("Don't ask again" was checked).
                    // Inform the user they need to go to settings to enable it.
                    Toast.makeText(this, getString(R.string.permission_denied_permanently), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Shows a dialog explaining why the camera permission is needed.
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_title))
            .setMessage(getString(R.string.camera_permission_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Request the permission again
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PermissionHelper.REQUEST_NOTIFICATION_PERMISSION
                )
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }

    /**
     * Fetches the user's reports from the server using a Volley StringRequest.
     * @param highlightId Optional ID of the report to highlight in the list.
     */
    private fun fetchReports(highlightId: Int? = null) {

        // Create a GET request to the reports URL
        val request = object : StringRequest(
            Method.GET, REPORTER_MY_REPORTS_URL,
            { response ->
                try {
                    // Parse the JSON response
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success){
                        val reports = mutableListOf<Report>()

                        // Get the JSON array of reports
                        val jsonArray = jsonObject.getJSONArray(REPORTS)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            
                            // Create a Report object from the JSON data
                            val report = Report(
                                id = obj.getInt(ID),
                                status = obj.getString(STATUS),
                                totalPrice = obj.getDouble(TOTAL_PRICE),
                                wasteItems = emptyList(),
                                date = formatDate(obj.getString(CREATED_AT))
                            )

                            // Add the report to the list
                            reports.add(report)
                        }

                        // 🔄 Sort reports: in_progress first
                        val sortedReports = reports.sortedByDescending { it.status == "in_progress" }

                        // 🔄 Update the RecyclerView adapter with the new data
                        reportsAdapter.updateReports(sortedReports, highlightId)
                        
                        // Show/Hide empty state
                        if (reports.isEmpty()) {
                            binding.emptyStateLayout.visibility = View.VISIBLE
                            binding.reportsRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.reportsRecyclerView.visibility = View.VISIBLE
                            
                            // If we have a highlight ID, scroll to it (it's likely at the top due to status)
                            if (highlightId != null) {
                                binding.reportsRecyclerView.scrollToPosition(0)
                            }
                        }
                        
                    }else{
                        // Handle the case where the server returns a "success: false" response
                        val message = jsonObject.getString(MESSAGE)
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .create()
                        dialog.show()
                        
                        binding.emptyStateLayout.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // Handle JSON parsing errors
                    e.printStackTrace()
                    Toast.makeText(this, getString(R.string.parsing_error, e.message), Toast.LENGTH_SHORT).show()
                    binding.emptyStateLayout.visibility = View.VISIBLE
                }

                // Stop the swipe refresh animation
                binding.swipeRefreshLayout.isRefreshing = false
            },
            { error ->
                // Handle network errors (e.g., no internet, server not found)
                error.printStackTrace()

                // Stop the swipe refresh animation
                binding.swipeRefreshLayout.isRefreshing = false
                
                if (reportsAdapter.itemCount == 0) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                }
            }
        ) {
            // Override getHeaders to add the authorization token
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
                // Add the Authorization header with the Bearer token from SharedPreferences
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, null)}"
                return headers
            }
        }

        // Add the request to the Volley queue
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Formats an ISO 8601 date string into a more readable format.
     * @param isoString The date string in ISO 8601 format.
     * @return The formatted date string, or the original string if formatting fails.
     */
    fun formatDate(isoString: String): String {
        return try {
            // Server's date format (ISO 8601)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")

            // User-friendly output format
            val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

            // Parse the ISO string and format it to the output string
            val date = isoFormat.parse(isoString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            // Fallback: return the original string if there's an error
            isoString
        }
    }

    override fun onReportStatusChanged(newReportId: Int?) {
        fetchReports(newReportId)
    }
}
