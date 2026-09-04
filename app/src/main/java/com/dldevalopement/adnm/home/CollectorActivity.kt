package com.dldevalopement.adnm.home

// Import necessary Android, Google Maps, Volley, and local classes
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.GPSUtils
import com.dldevalopement.adnm.PermissionHelper
import com.dldevalopement.adnm.ProfileActivity
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.ActivityCollectorBinding
import com.dldevalopement.adnm.manager.AdManager
import com.dldevalopement.adnm.manager.AuthManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONArray
import org.json.JSONObject

/**
 * Main activity for the 'Collector' role.
 * This activity displays a map with reported waste locations and manages user interactions.
 * It implements OnMapReadyCallback to handle map initialization.
 */
class CollectorActivity : AppCompatActivity(), OnMapReadyCallback {

    // View binding instance for safe access to views
    private lateinit var _binding: ActivityCollectorBinding
    private val binding get() = _binding

    // Google Map instance
    private lateinit var mMap: GoogleMap
    // Client for retrieving the user's last known location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Persistent Bottom Sheet Behavior
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val weightInputs = mutableMapOf<String, EditText>()
    private val wasteTypesID = mutableListOf<String>()
    private val wasteTypesNames = mutableListOf<String>()

    /**
     * Called when the activity is first created.
     * Initializes the UI and sets up the map.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCollectorBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize AdMob
        AdManager.initialize(this)
        // Create and add the banner ad to the container
        val adView = AdManager.createBannerAd(this)
        binding.adContainer.addView(adView)

        // Set a click listener for the logout button
        binding.logoutButton.setOnClickListener {
            AuthManager.logout(this)
        }

        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Request necessary permissions (notifications and location)
        PermissionHelper.requestPermissions(this)

        // Initialize the Fused Location Provider client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the map fragment and get the map asynchronously
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Persistent Bottom Sheet
        setupBottomSheet()
    }

    /**
     * Initializes the persistent bottom sheet behavior and its initial state.
     */
    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 0
    }

    /**
     * Handles the result of a permission request.
     * @param requestCode The request code.
     * @param permissions The requested permissions.
     * @param grantResults The grant results.
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
                // Permission granted, show a toast and enable location on the map
                Toast.makeText(this, getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show()
                if (::mMap.isInitialized) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true
                        getCurrentLocation() // Get the location as soon as permission is granted
                    }
                }
            } else {
                // Permission denied, show a toast
                Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }


    /**
     * Callback for when the map is ready to be used.
     * @param googleMap The GoogleMap instance.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("MAP", "map ready")
        mMap = googleMap

        // Apply custom map style (Light Green Theme)
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style
                )
            )
            if (!success) {
                Log.e("MAP", "Style parsing failed.")
            }
        } catch (e: Exception) {
            Log.e("MAP", "Can't find style. Error: ", e)
        }

        // Enable zoom controls and check for location permission to enable my-location layer
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            // Set a click listener on the My Location button to check GPS status
            mMap.setOnMyLocationButtonClickListener {
                if (!GPSUtils.isGPSEnabled(this)) {
                    GPSUtils.showGPSDialog(this)
                    true // Block the default behavior
                } else {
                    false // Allow the default behavior (move camera to location)
                }
            }
            getCurrentLocation()
        }
        // Fetch reports to display on the map
        fetchReports()

        // Set up the refresh button to reload the reports
        binding.refreshButton.setOnClickListener {
            loadReports()
        }
    }

    /**
     * Retrieves the user's current location and moves the map camera to that position.
     */
    private fun getCurrentLocation() {
        if (!GPSUtils.isGPSEnabled(this)) {
            GPSUtils.showGPSDialog(this)
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Toast.makeText(this, getString(R.string.couldnt_get_location), Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    /**
     * Fetches accepted reports from the server and adds them as markers on the map.
     */
    private fun fetchReports() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val request = object : StringRequest(
            Method.GET, COLLECTOR_REPORTS_URL,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success) {
                        Log.i("DATA", "DONE")
                        val jsonArray = jsonObject.getJSONArray(REPORTS)
                        var inProgressReport: JSONObject? = null

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val lat = obj.getDouble("latitude")
                            val lng = obj.getDouble("longitude")
                            val status = obj.getString("status")

                            if (status == "in_progress") {
                                inProgressReport = obj
                            }

                            val markerColor: Float
                            val zIndex: Float
                            if (status == "in_progress"){
                                markerColor = BitmapDescriptorFactory.HUE_ORANGE
                                zIndex = 1.0f // Show on top of others
                                drawRealRoute(lat, lng)
                            } else {
                                markerColor = BitmapDescriptorFactory.HUE_GREEN
                                zIndex = 0.0f
                            }

                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lng))
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                                    .zIndex(zIndex)
                            )

                            // هنا خزّن كل البيانات الخاصة بالبلاغ
                            marker?.tag = obj
                        }

                        // Automatically open bottom sheet if an in-progress report is found
                        inProgressReport?.let {
                            bindReportToSheet(it)
                        }

                        // marker listener
                        mMap.setOnMarkerClickListener { marker ->
                            val obj = marker.tag as? JSONObject ?: return@setOnMarkerClickListener true
                            bindReportToSheet(obj)
                            true
                        }
                    } else {
                        // Handle server-side error messages
                        val message = jsonObject.getString(MESSAGE)
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .create()
                        dialog.show()
                    }
                } catch (e: Exception) {
                    // Handle JSON parsing errors
                    Log.e("DATA", "Parsing error: ${e.message}")
                    Toast.makeText(this, getString(R.string.parsing_error, e.message), Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = android.view.View.GONE
                }
            },
            { _ ->
                // Handle network errors
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, getString(R.string.failed_to_load_reports), Toast.LENGTH_SHORT).show()
            }
        ) {
            // Add authorization and accept headers
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE)
                headers[ACCEPT] = APPLICATION_JSON
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, null)}"
                return headers
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Binds report data to the persistent bottom sheet and shows it.
     */
    private fun bindReportToSheet(reportJson: JSONObject) {
        val sheetView = binding.bottomSheetLayout.root
        val id = reportJson.optInt("id")
        val status = reportJson.optString("status")
        val lat = reportJson.optString("latitude")
        val lng = reportJson.optString("longitude")

        // Reset inputs
        weightInputs.clear()
        wasteTypesID.clear()
        wasteTypesNames.clear()

        // UI Binding
        sheetView.findViewById<TextView>(R.id.tvTitle).text = "${getString(R.string.report)} #$id"
        sheetView.findViewById<TextView>(R.id.tvReportIdValue).text = id.toString()
        
        val tvStatusBadge = sheetView.findViewById<TextView>(R.id.tvStatusBadge)
        tvStatusBadge.text = status.replace("_", " ").replaceFirstChar { it.uppercase() }
        
        when (status) {
            "accepted" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.blue_drive))
            "in_progress" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.orange_status))
            "collected" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.primary_green))
        }

        // Expand/Collapse Logic
        val layoutIdHeader = sheetView.findViewById<LinearLayout>(R.id.layoutIdHeader)
        val layoutCollapsibleInfo = sheetView.findViewById<LinearLayout>(R.id.layoutCollapsibleInfo)
        val ivDropdownArrow = sheetView.findViewById<ImageView>(R.id.ivDropdownArrow)
        
        layoutCollapsibleInfo.visibility = View.GONE
        ivDropdownArrow.rotation = 90f

        layoutIdHeader.setOnClickListener {
            val isVisible = layoutCollapsibleInfo.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(sheetView as ViewGroup)
            layoutCollapsibleInfo.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivDropdownArrow.animate().rotation(if (isVisible) 90f else 270f).start()
        }

        setupRow(sheetView.findViewById(R.id.rowId), R.drawable.ic_hash, getString(R.string.id), id.toString())
        setupRow(sheetView.findViewById(R.id.rowStatus), R.drawable.ic_check, getString(R.string.status), status.replace("_", " ").replaceFirstChar { it.uppercase() })

        val userObj = reportJson.optJSONObject("user")
        val userName = userObj?.let { "${it.optString("name")} ${it.optString("last_name")}" } ?: "N/A"
        val userPhone = userObj?.optString("phone_number") ?: "N/A"

        setupRow(sheetView.findViewById(R.id.rowName), R.drawable.ic_person, getString(R.string.user_name), userName)
        setupRow(sheetView.findViewById(R.id.rowPhone), R.drawable.ic_phone, getString(R.string.phone_number), userPhone)

        val containerWasteTypes = sheetView.findViewById<LinearLayout>(R.id.containerWasteTypes)
        val containerWeightInputs = sheetView.findViewById<LinearLayout>(R.id.containerWeightInputs)
        containerWasteTypes.removeAllViews()
        containerWeightInputs.removeAllViews()

        val wasteArray = reportJson.optJSONArray("waste_types") ?: JSONArray()
        for (i in 0 until wasteArray.length()) {
            val wasteObj = wasteArray.getJSONObject(i)
            val wId = wasteObj.optString("id")
            val wType = wasteObj.optString("type", "Waste Type")
            
            wasteTypesID.add(wId)
            wasteTypesNames.add(wType)

            val rowView = LayoutInflater.from(this).inflate(R.layout.item_report_info_row, containerWasteTypes, false)
            setupRow(rowView, R.drawable.ic_trash, getString(R.string.type), wType)
            containerWasteTypes.addView(rowView)

            if (status == "in_progress") {
                val weightInputView = LayoutInflater.from(this).inflate(R.layout.item_weight_input, containerWeightInputs, false)
                val tilWeight = weightInputView.findViewById<TextInputLayout>(R.id.tilWeight)
                val etWeight = weightInputView.findViewById<EditText>(R.id.etWeight)
                
                tilWeight.hint = "${getString(R.string.please_enter_weight)} $wType"
                weightInputs[wType] = etWeight
                containerWeightInputs.addView(weightInputView)
            }
        }

        // Action Buttons
        val btnDrive = sheetView.findViewById<Button>(R.id.btnDrive)
        val btnCall = sheetView.findViewById<Button>(R.id.btnCall)
        val btnPositive = sheetView.findViewById<Button>(R.id.btnPositive)
        val btnClose = sheetView.findViewById<Button>(R.id.btnClose)
        val btnLocate = sheetView.findViewById<Button>(R.id.btnLocate)

        btnLocate.setOnClickListener {
            val reportLatLng = LatLng(lat.toDouble(), lng.toDouble())
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(reportLatLng, 17f))
        }

        btnDrive.visibility = if (status == "in_progress") View.VISIBLE else View.GONE
        btnDrive.setOnClickListener { openNavigation(lat, lng) }

        btnCall.setOnClickListener {
            if (userPhone != "N/A") {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$userPhone")))
            }
        }
        btnClose.setOnClickListener { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }

        when (status) {
            "accepted" -> {
                btnPositive.visibility = View.VISIBLE
                btnPositive.text = getString(R.string.start_collecting)
                btnPositive.setOnClickListener { updateReportStatus(id, "in_progress", JSONArray()) }
            }
            "in_progress" -> {
                btnPositive.visibility = View.VISIBLE
                btnPositive.text = getString(R.string.collected)
                btnPositive.setOnClickListener {
                    val wastes = collectWeights()
                    if (wastes != null) {
                        updateReportStatus(id, "collected", wastes)
                    }
                }
            }
            else -> btnPositive.visibility = View.GONE
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupRow(view: View, iconRes: Int, label: String, value: String) {
        view.findViewById<ImageView>(R.id.ivIcon).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvLabel).text = label
        view.findViewById<TextView>(R.id.tvValue).text = value
    }

    private fun openNavigation(lat: String, lng: String?) {
        if (lat != "0.0000000" && lng != "0.0000000" && lng != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=d"))
            intent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")))
            }
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectWeights(): JSONArray? {
        val wastesArray = JSONArray()
        for (i in wasteTypesID.indices) {
            val wType = wasteTypesNames[i]
            val value = weightInputs[wType]?.text.toString().trim()
            if (value.isEmpty()) {
                Toast.makeText(this, "${getString(R.string.please_enter_weight)} $wType", Toast.LENGTH_SHORT).show()
                return null
            }
            val wasteObj = JSONObject().apply {
                put("id", wasteTypesID[i])
                put("weight", value.toDouble())
            }
            wastesArray.put(wasteObj)
        }
        return wastesArray
    }

    private fun updateReportStatus(id: Int, newStatus: String, wastes: JSONArray?) {
        val body = JSONObject().apply {
            put("report_id", id)
            put("status", newStatus)
            put("wastes", wastes ?: JSONArray())
        }

        val request = object : JsonObjectRequest(Method.POST, COLLECTOR_UPDATE_STATUS_URL, body,
            Response.Listener { response ->
                if (response.optBoolean(SUCCESS)) {
                    loadReports()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    Toast.makeText(this, response.optString(MESSAGE), Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val token = getSharedPreferences(DATA, Context.MODE_PRIVATE).getString(TOKEN, "")
                return mutableMapOf(AUTHORIZATION to "$BEARER $token", "Accept" to "application/json")
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Draws a route from the user's current location to a given report location using the OSRM routing service.
     * @param lat The latitude of the report.
     * @param lng The longitude of the report.
     */
    private fun drawRealRoute(lat: Double, lng: Double) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Construct the URL for the OSRM routing API (using HTTPS and lon,lat format)
                val url =
                    "https://router.project-osrm.org/route/v1/driving/${location.longitude},${location.latitude};${lng},${lat}?overview=full&geometries=geojson"

                val request = object : StringRequest(Method.GET, url,
                    { response ->
                        try {
                            val json = JSONObject(response)
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                                val coords = geometry.getJSONArray("coordinates")

                                // Create a polyline to draw the route
                                val polylineOptions =
                                    PolylineOptions().color(Color.BLUE).width(8f)

                                // Add each coordinate to the polyline
                                for (i in 0 until coords.length()) {
                                    val coord = coords.getJSONArray(i)
                                    val long = coord.getDouble(0)
                                    val lati = coord.getDouble(1)
                                    polylineOptions.add(LatLng(lati, long))
                                }
                                // Add the polyline to the map
                                mMap.addPolyline(polylineOptions)
                            }
                        } catch (e: Exception) {
                            Log.e("OSRM", "Error parsing routing response: ${e.message}")
                        }
                    },
                    { error ->
                        // Handle routing API errors (like 400 NoRoute or 403 Forbidden)
                        val response = error.networkResponse
                        if (response != null && response.data != null) {
                            val errorStr = String(response.data)
                            Log.e("OSRM", "Status Code: ${response.statusCode}, Error: $errorStr")
                            // 400 often means NoRoute (e.g. points are on different continents)
                        } else {
                            Log.e("OSRM", "Routing error: ${error.message}")
                        }
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        // OSRM demo server requires a User-Agent header
                        headers["User-Agent"] = "ADNM-Android-App"
                        return headers
                    }
                }

                Volley.newRequestQueue(this).add(request)
            }
        }
    }


    /**
     * Clears all markers and polylines from the map and re-fetches the reports.
     */
    private fun loadReports() {
        Log.d("CollectorMap", "loadReports called")
        mMap.clear()
        fetchReports()
    }
}
