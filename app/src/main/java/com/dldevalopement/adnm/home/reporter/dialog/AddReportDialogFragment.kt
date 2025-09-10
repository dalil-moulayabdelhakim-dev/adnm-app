package com.dldevalopement.adnm.home.reporter.dialog

// Import necessary Android, Volley, and local classes
import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.GPSUtils
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.DialogAddReportBinding
import com.dldevalopement.adnm.home.collector.dialog.ReportInfoDialogFragment.ReportStatusListener
import com.dldevalopement.adnm.home.reporter.WasteType
import com.dldevalopement.adnm.manager.AuthManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject

/**
 * A DialogFragment for reporters to add a new waste report.
 * It manages UI, API calls, and location services.
 * @param context The context of the calling activity.
 */
class AddReportDialogFragment(private val context: Context) : DialogFragment() {

    // View binding instance for safe access to views
    private var _binding: DialogAddReportBinding? = null
    private val binding get() = _binding!!

    // List to hold fetched waste types and the currently selected one
    private var wasteTypes: List<WasteType> = listOf()

    // Client for getting the user's last known location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variables to store the user's current latitude and longitude
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private var isVerified = false

    private val selectedWasteTypes = mutableListOf<WasteType>()


    /**
     * An interface to define a callback for when the report's status is changed.
     * This allows the calling activity to refresh its UI after the update.
     */
    interface ReportStatusListener {
        fun onReportStatusChanged()
    }

    private var listener: ReportStatusListener? = null


    /**
     * Sets the listener for status changes.
     */
    fun setReportStatusListener(l: ReportStatusListener) {
        listener = l
    }

    /**
     * Creates and configures the AlertDialog that will be displayed.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for this dialog fragment
        _binding = DialogAddReportBinding.inflate(LayoutInflater.from(context))

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Fetch waste types and get the user's current location upon creation
        fetchWasteTypes()
        getCurrentLocation()

        binding.recaptchaButton.setOnClickListener {
            verify(context, binding.recaptchaProgressbar, binding.recaptchaCheckbox, binding.errorIc)
        }

        binding.recaptchaCheckbox.setOnClickListener {
            verify(context, binding.recaptchaProgressbar, binding.recaptchaCheckbox, binding.errorIc)
        }

        // Build and return the AlertDialog
        return AlertDialog.Builder(context, R.style.dialog)
            .setTitle(context.getString(R.string.add_report))
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.submit)) { _, _ ->
                // Validate that a waste type is selected and weight is entered
                    val manager =
                        context.getSystemService(Context.LOCATION_SERVICE) as (LocationManager)
                    // Check if GPS is enabled before sending the report
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.enable_gps),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Send the report with the collected data
                        if (isVerified)
                        sendReport( currentLat ?: 0.0, currentLng ?: 0.0)
                        else
                            Toast.makeText(context, context.getString(R.string.check_the_box_message), Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
    }

    /**
     * Fetches the list of waste types from the server and populates the Spinner.
     */
    private fun fetchWasteTypes() {
        val request = object : StringRequest(
            Method.GET,
            GET_WASTE_TYPES_URL,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success) {
                        val jsonArray = jsonObject.getJSONArray(WASTE_TYPES)
                        val tempList = mutableListOf<WasteType>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.getInt(ID)
                            val name = obj.getString(TYPE)
                            val pricePerKg = obj.getDouble(PRICE)
                            tempList.add(WasteType(id, name, pricePerKg))
                        }

                        wasteTypes = tempList.sortedBy { it.id }

                        // ننظف الكونتينر قبل ما نضيفو
                        binding.checkboxContainer.removeAllViews()

                        // نضيف CheckBox لكل نوع نفايات
                        for (waste in wasteTypes) {
                            val checkBox = CheckBox(context)
                            checkBox.text = waste.name
                            checkBox.tag = waste   // نخزنو الـ Object كامل في tag

                            checkBox.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    // تمت الإضافة
                                    selectedWasteTypes.add(waste)
                                } else {
                                    // تمت الإزالة
                                    selectedWasteTypes.remove(waste)
                                }
                            }

                            binding.checkboxContainer.addView(checkBox)
                        }

                    } else {
                        val message = jsonObject.getString(MESSAGE)
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton(context.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .create().show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, context.getString(R.string.parsing_error), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                dismiss()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences(DATA, MODE_PRIVATE)
                val headers = HashMap<String, String>()
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, "")}"
                return headers
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    /**
     * Fetches the user's last known location. It requests permission if not granted and
     * checks if GPS is enabled.
     */
    private fun getCurrentLocation() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if they are not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        // Check if GPS is enabled using a utility function
        if (!GPSUtils.isGPSEnabled(context)) {
            GPSUtils.showGPSDialog(context)
        } else {
            // Get the last known location and store it
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLat = location.latitude
                        currentLng = location.longitude
                    }
                }
        }
    }

    /**
     * Sends the report data to the server via a Volley POST request.
     * @param weight The weight of the waste.
     * @param lat The latitude of the report location.
     * @param lng The longitude of the report location.
     */
    private fun sendReport(lat: Double, lng: Double) {
        if (selectedWasteTypes.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.select_waste_type), Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(context).apply { show() }
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(DATA, MODE_PRIVATE)

        // نحضر JSON Body
        val selectedIds = selectedWasteTypes.map { it.id }
        val jsonBody = JSONObject().apply {
            put("latitude", lat.toString())
            put("longitude", lng.toString())
            put("waste_type_ids", JSONArray(selectedIds))
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            REPORTER_ADD_REPORT_URL,
            jsonBody,
            { response ->
                progressDialog.dismiss()
                Toast.makeText(context, response.optString(MESSAGE, "Report added"), Toast.LENGTH_SHORT).show()
                listener?.onReportStatusChanged()
                dismiss()
            },
            { error ->
                progressDialog.dismiss()
                Log.e("API_DEBUG", "Error: ${error.message}", error)
                Toast.makeText(context, "${context.getString(R.string.failed)}: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json"
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, "")}"
                return headers
            }
        }

        Volley.newRequestQueue(context).add(request)
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
                    Toast.makeText(context, context.getString(R.string.verification_failed), Toast.LENGTH_SHORT).show()
                }
                checkBox.isChecked = success

            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                errorIC.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                checkBox.visibility = View.GONE
                Toast.makeText(context, "${context.getString(R.string.error)}: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val safeToken = context.getString(R.string.recaptcha_site_key)
                return mapOf(TOKEN to safeToken)
            }
        }
        queue.add(request)
    }

    /**
     * Cleans up the view binding instance to avoid memory leaks.
     */
    override fun onDestroyView() {
        listener = null
        _binding = null
        super.onDestroyView()
    }
}