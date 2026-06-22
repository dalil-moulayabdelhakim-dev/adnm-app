package com.dldevalopement.adnm.home.reporter.dialog

// Import necessary Android, Volley, and local classes
import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.GPSUtils
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.DialogAddReportBinding
import com.dldevalopement.adnm.home.reporter.WasteType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * A DialogFragment for reporters to add a new waste report.
 * It manages UI, API calls, and location services.
 * @param context The context of the calling activity.
 */
class AddReportDialogFragment(private val context: Context) : DialogFragment(), OnMapReadyCallback {

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

    private val wasteImagesBase64 = HashMap<Int, String>() // Key: Waste ID, Value: Base64 String
    private var currentWasteIdForPhoto: Int? = null

    private var isVerified = false

    private val selectedWasteTypes = mutableListOf<WasteType>()

    private val wasteStatusViews = HashMap<Int, TextView>()

    private var googleMap: GoogleMap? = null

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

        // Initialize MapView
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

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

        binding.btnRefreshLocation.setOnClickListener {
            getCurrentLocation(true)
        }

        // Build and return the AlertDialog
        val dialog = AlertDialog.Builder(context, R.style.dialog)
            .setTitle(context.getString(R.string.add_report))
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.submit), null) // نضع null هنا مؤقتاً
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()

        // هذا الجزء هو السر: نمنع الديالوج من الانغلاق
        dialog.setOnShowListener {
            val submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            submitButton.setOnClickListener {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(context, context.getString(R.string.enable_gps), Toast.LENGTH_SHORT).show()
                } else {
                        sendReport(currentLat ?: 0.0, currentLng ?: 0.0)
                }
                // لاحظ: لا يوجد dismiss() هنا، لذا الديالوج سيبقى مفتوحاً حتى نغلقه نحن يدوياً عند النجاح فقط
            }
        }

        return dialog
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        updateMapLocation()
    }

    private fun updateMapLocation() {
        if (googleMap != null && currentLat != null && currentLng != null) {
            val userLatLng = LatLng(currentLat!!, currentLng!!)
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(userLatLng).title("Your Location"))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f))
        }
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
                            val nameEn = obj.getString(TYPE_EN)
                            val nameAr = obj.getString(TYPE_AR)
                            val nameFr = obj.getString(TYPE_FR)
                            val pricePerKg = obj.getDouble(PRICE)
                            tempList.add(WasteType(id, nameEn, nameAr, nameFr, pricePerKg))
                        }

                        wasteTypes = tempList.sortedBy { it.id }

                        // ننظف الكونتينر قبل ما نضيفو
                        _binding?.checkboxContainer?.removeAllViews()

                        // تحديد لغة الجهاز الحالية
                        val currentLang = Locale.getDefault().language

                        // نضيف CheckBox لكل نوع نفايات
                        for (waste in wasteTypes) {
                            val layout = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }

                            val checkBox = CheckBox(context).apply {
                                // الحصول على الاسم بناءً على اللغة
                                val localizedName = waste.getNameByLang(currentLang)
                                
                                val namePrefix = "$localizedName - "
                                val price = "${waste.pricePerKg}"
                                val unit = " ${context.getString(R.string.da_per_kg)}"

                                // دمج النصوص بالترتيب
                                val fullText = namePrefix + price + unit

                                // إنشاء SpannableString لتلوين جزء من النص
                                val spannable = android.text.SpannableString(fullText)

                                // تحديد بداية ونهاية السعر داخل النص الكامل
                                val start = namePrefix.length
                                val end = start + price.length

                                // تطبيق اللون الأخضر على السعر فقط
                                spannable.setSpan(
                                    android.text.style.ForegroundColorSpan(context.getColor(R.color.green)),
                                    start,
                                    end,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                // تطبيق النص المنسق على الـ CheckBox
                                text = spannable

                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            val imgButton = ImageView(context).apply {
                                setImageResource(R.drawable.ic_camera)
                                visibility = View.GONE
                                setPadding(15, 10, 15, 10)
                            }

                            val statusText = TextView(context).apply {
                                text = ""
                                textSize = 12f
                                setTextColor(context.getColor(R.color.green))
                                visibility = View.GONE
                                setPadding(10, 0, 10, 0)
                            }

                            wasteStatusViews[waste.id] = statusText

                            imgButton.setOnClickListener {
                                openCameraForWaste(waste.id)
                            }

                            checkBox.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    selectedWasteTypes.add(waste)
                                    imgButton.visibility = View.VISIBLE
                                    statusText.visibility = View.VISIBLE
                                } else {
                                    selectedWasteTypes.remove(waste)
                                    wasteImagesBase64.remove(waste.id)
                                    imgButton.visibility = View.GONE
                                    statusText.visibility = View.GONE
                                    statusText.text = ""
                                }
                            }

                            // الترتيب الجديد لضمان ظهور النص على يسار الأيقونة
                            layout.addView(checkBox)   // يأخذ المساحة الكبرى على اليسار
                            layout.addView(statusText) // يظهر بعد التشيك بوكس (على يسار الأيقونة)
                            layout.addView(imgButton)  // يظهر في أقصى اليمين

                            _binding?.checkboxContainer?.addView(layout)
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
                    Toast.makeText(context, context.getString(R.string.parsing_error, e.message), Toast.LENGTH_SHORT).show()
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
     * Fetches the user's current location. It requests permission if not granted and
     * checks if GPS is enabled.
     */
    private fun getCurrentLocation(isRefresh: Boolean = false) {
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
            if (isRefresh) {
                Toast.makeText(context, context.getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            currentLat = location.latitude
                            currentLng = location.longitude
                            updateMapLocation()
                        } else {
                             Toast.makeText(context, context.getString(R.string.couldnt_get_location), Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // Get the last known location and store it
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            currentLat = location.latitude
                            currentLng = location.longitude
                            updateMapLocation()
                        } else {
                            // If last location is null, try getting a fresh one
                            getCurrentLocation(true)
                        }
                    }
            }
        }
    }

    /**
     * Sends the report data to the server via a Volley POST request.
     * @param lat The latitude of the report location.
     * @param lng The longitude of the report location.
     */
    // Function to send a waste report with location and selected waste types
    private fun sendReport(lat: Double, lng: Double) {
        // Check if the user selected any waste types before sending
        if (selectedWasteTypes.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.select_waste_type), Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedWasteTypes.any { !wasteImagesBase64.containsKey(it.id) }) {
            Toast.makeText(context, context.getString(R.string.please_take_a_picture_for_each_waste_type), Toast.LENGTH_SHORT).show()
            return
        }

        if (!isVerified){
            Toast.makeText(context, context.getString(R.string.check_the_box_message), Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog while sending the report
        val progressDialog = ProgressDialog(context).apply { show() }
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(DATA, MODE_PRIVATE)

        // Prepare JSON body for the API request
        //val selectedIds = selectedWasteTypes.map { it.id }
        val wasteDataArray = JSONArray()
        for (waste in selectedWasteTypes) {
            val obj = JSONObject()
            obj.put("id", waste.id)
            obj.put("image", wasteImagesBase64[waste.id] ?: "") // إرسال الصورة كـ string
            wasteDataArray.put(obj)
        }
        val jsonBody = JSONObject().apply {
            put("latitude", lat.toString())
            put("longitude", lng.toString())
            put("waste_data", wasteDataArray) // استبدلنا waste_type_ids بـ waste_data
        }

        // Create a POST request using Volley
        val request = object : JsonObjectRequest(
            Method.POST,
            REPORTER_ADD_REPORT_URL,
            jsonBody,
            { response ->
                // Handle successful response
                progressDialog.dismiss()
                Toast.makeText(context, response.optString(MESSAGE, "Report added"), Toast.LENGTH_SHORT).show()
                listener?.onReportStatusChanged()
                dismiss()
            },
            { error ->
                // Handle network or server error
                progressDialog.dismiss()
                Log.e("API_DEBUG", "Error: ${error.message}", error)
                Toast.makeText(context, "${context.getString(R.string.failed)}: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            // Add headers including authorization token
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json"
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, "")}"
                Log.i("API_DEBUG", "Headers: ${headers[AUTHORIZATION]}")
                return headers
            }
        }

        // Add the request to the queue for execution
        Volley.newRequestQueue(context).add(request)
    }

    // Function to verify user using reCAPTCHA
    private fun verify(context: Context, progressBar: ProgressBar, checkBox: CheckBox, errorIC: ImageView){
        // Show loading indicator and hide the checkbox initially
        progressBar.visibility = View.VISIBLE
        checkBox.visibility = View.GONE
        val queue = Volley.newRequestQueue(context)

        // Create a POST request to verify reCAPTCHA
        val request = object : StringRequest(
            Method.POST, RECAPTCHA_URL,
            Response.Listener { res ->
                // Parse the response
                val jsonObject = JSONObject(res)
                val success = jsonObject.getBoolean(SUCCESS)
                isVerified = success

                if (success){
                    // If verification succeeded, update UI
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    checkBox.isEnabled = false
                    _binding?.recaptchaButton?.isEnabled = false
                } else {
                    // If verification failed, show error message
                    progressBar.visibility = View.GONE
                    checkBox.visibility = View.VISIBLE
                    Toast.makeText(context, context.getString(R.string.verification_failed), Toast.LENGTH_SHORT).show()
                }

                // Update checkbox state
                checkBox.isChecked = success
            },
            Response.ErrorListener { error ->
                // Handle network or API errors
                error.printStackTrace()
                errorIC.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                checkBox.visibility = View.GONE
                Toast.makeText(context, "${context.getString(R.string.error)}: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            // Send required parameters for verification
            override fun getParams(): Map<String, String> {
                val safeToken = context.getString(R.string.recaptcha_site_key)
                return mapOf(TOKEN to safeToken)
            }
        }

        // Add request to the queue
        queue.add(request)
    }

    // في بداية الكلاس
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && currentWasteIdForPhoto != null) {
            // تحويل الصورة لـ Base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            // حفظها في الماب
            wasteImagesBase64[currentWasteIdForPhoto!!] = base64Image

            // --- التعديل هنا: تحديث النص بجانب الكاميرا ---
            wasteStatusViews[currentWasteIdForPhoto!!]?.apply {
                text = context.getString(R.string.image_captured)
            }

            Toast.makeText(context, context.getString(R.string.image_captured_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCameraForWaste(wasteId: Int) {
        currentWasteIdForPhoto = wasteId
        takePictureLauncher.launch(null)
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding?.mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }

    /**
     * Cleans up the view binding instance to avoid memory leaks.
     */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
