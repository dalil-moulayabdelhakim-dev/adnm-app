package com.dldevalopement.adnm.home

// Import necessary Android, Google Maps, Volley, and local classes
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.GPSUtils
import com.dldevalopement.adnm.PermissionHelper
import com.dldevalopement.adnm.ProfileActivity
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.ActivityCollectorBinding
import com.dldevalopement.adnm.home.collector.dialog.ReportInfoDialogFragment
import com.dldevalopement.adnm.manager.AuthManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject

/**
 * Main activity for the 'Collector' role.
 * This activity displays a map with reported waste locations and manages user interactions.
 * It implements OnMapReadyCallback to handle map initialization and ReportDialogFragment.ReportStatusListener
 * to handle updates from the dialog.
 */
class CollectorActivity : AppCompatActivity(), OnMapReadyCallback,
    ReportInfoDialogFragment.ReportStatusListener {

    // View binding instance for safe access to views
    private lateinit var _binding: ActivityCollectorBinding
    private val binding get() = _binding

    // Google Map instance
    private lateinit var mMap: GoogleMap
    // Client for retrieving the user's last known location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * Called when the activity is first created.
     * Initializes the UI and sets up the map.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCollectorBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val lat = obj.getDouble("latitude")
                            val lng = obj.getDouble("longitude")
                            val status = obj.getString("status")

                            val markerColor: Float
                            if (status == "in_progress"){
                                    markerColor = BitmapDescriptorFactory.HUE_ORANGE
                                drawRealRoute(lat, lng)
                            }else {
                                markerColor = BitmapDescriptorFactory.HUE_GREEN
                            }

                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lng))
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                            )

                            // هنا خزّن كل البيانات الخاصة بالبلاغ
                            marker?.tag = obj
                        }
// listener
                        mMap.setOnMarkerClickListener { marker ->
                            val obj = marker.tag as? JSONObject ?: return@setOnMarkerClickListener true

                            val id = obj.getInt("id")
                            val status = obj.getString("status")
                            val wasteArray = obj.getJSONArray("waste_types")

                            val wasteTypes = mutableListOf<String>()
                            val wasteTypesID = mutableListOf<String>()
                            for (i in 0 until wasteArray.length()) {
                                val wastJOB = wasteArray.getJSONObject(i)
                                wasteTypes.add(wastJOB.getString(TYPE))
                                wasteTypesID.add(wastJOB.getString(ID))
                            }

                            if (status == "accepted") {
                                val dialog = ReportInfoDialogFragment(
                                    this, id, emptyList(), emptyList(), status
                                )
                                dialog.setReportStatusListener(this)
                                dialog.show(supportFragmentManager, "ReportInfoDialog")
                            } else if (status == "in_progress") {
                                val dialog = ReportInfoDialogFragment(
                                    this, id, wasteTypesID, wasteTypes, status
                                )
                                dialog.setReportStatusListener(this)
                                dialog.show(supportFragmentManager, "ReportInputDialog")
                            }
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
     * Draws a route from the user's current location to a given report location using the OSRM routing service.
     * @param lat The latitude of the report.
     * @param lng The longitude of the report.
     */
    private fun drawRealRoute(lat: Double, lng: Double) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Construct the URL for the OSRM routing API
                val url =
                    "http://router.project-osrm.org/route/v1/driving/${location.longitude},${location.latitude};${lng},${lat}?overview=full&geometries=geojson"

                val request = object : StringRequest(Method.GET, url,
                    { response ->
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
                    },
                    { error ->
                        // Handle routing API errors
                        error.printStackTrace()
                    }) {}

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

    /**
     * Callback method from the ReportDialogFragment listener.
     * This is triggered when a report's status is successfully changed.
     * It calls `loadReports` to refresh the map with the latest data.
     */
    override fun onReportStatusChanged() {
        Log.d("CollectorMap", "onReportStatusChanged called")
        loadReports()
    }
}
