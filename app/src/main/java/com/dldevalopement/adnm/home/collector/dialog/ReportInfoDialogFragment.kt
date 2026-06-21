package com.dldevalopement.adnm.home.collector.dialog

// Import necessary Android and Volley classes
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * A DialogFragment that displays a report's details to a 'Collector' user.
 * It allows the user to update the report's status and view reporter information.
 */
class ReportInfoDialogFragment(
    private val context: Context,
    private val reportJson: JSONObject
) : DialogFragment() {

    /**
     * An interface to define a callback for when the report's status is changed.
     */
    interface ReportStatusListener {
        fun onReportStatusChanged()
    }

    private var listener: ReportStatusListener? = null
    private val weightInputs = mutableMapOf<String, EditText>()
    private val wasteTypesID = mutableListOf<String>()
    private val wasteTypesNames = mutableListOf<String>()

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
        val builder = AlertDialog.Builder(context, R.style.dialog)

        val id = reportJson.optInt("id")
        val status = reportJson.optString("status")
        val lat = reportJson.optString("latitude")
        val lng = reportJson.optString("longitude")

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_report_info_collector, null)
        
        val container = view.findViewById<LinearLayout>(R.id.containerWasteTypes)
        view.findViewById<TextView>(R.id.tvId).text = "${context.getString(R.string.id)}: $id"
        view.findViewById<TextView>(R.id.tvStatus).text = "${context.getString(R.string.status)}: $status"

        val btnDrive = view.findViewById<Button>(R.id.btnDrive)
        if (status == "in_progress") {
            btnDrive.visibility = View.VISIBLE
            btnDrive.setOnClickListener {
                openNavigation(lat, lng)
            }
        }

        // User Info (Reporter)
        val userObj = reportJson.optJSONObject("user")
        val userName = userObj?.let { "${it.optString("name")} ${it.optString("last_name")}" } ?: ""
        val userPhone = userObj?.optString("phone_number") ?: ""
        Log.i("USER", "json: $reportJson")
        view.findViewById<TextView>(R.id.tvUserName).text = "${context.getString(R.string.name)}: $userName"
        view.findViewById<TextView>(R.id.tvUserPhone).text = "${context.getString(R.string.phone_number)}: $userPhone"

        view.findViewById<Button>(R.id.btnCall).setOnClickListener {
            if (userPhone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$userPhone")
                startActivity(intent)
            } else {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Waste Types
        val wasteArray = reportJson.optJSONArray("waste_types") ?: JSONArray()
        for (i in 0 until wasteArray.length()) {
            val wasteObj = wasteArray.getJSONObject(i)
            val wId = wasteObj.optString("id")
            // Try to get 'type' field, fallback to waste_type_id if missing
            val wType = when {
                wasteObj.has("type") -> wasteObj.optString("type")
                wasteObj.has("waste_type_id") -> "Type ID: ${wasteObj.optString("waste_type_id")}"
                else -> "Waste ${i + 1}"
            }
            
            wasteTypesID.add(wId)
            wasteTypesNames.add(wType)

            if (status == "in_progress") {
                val input = EditText(context).apply {
                    setBackgroundResource(R.drawable.shape_feilds)
                    hint = wType
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                input.layoutParams = params
                container.addView(input)
                weightInputs[wType] = input
            } else {
                val textView = TextView(context).apply {
                    text = wType
                    textSize = 16f
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 8)
                textView.layoutParams = params
                container.addView(textView)
            }
        }

        builder.setView(view)
        builder.setCancelable(false)
        builder.setTitle("${context.getString(R.string.report)} #$id")

        when (status) {
            "accepted" -> {
                builder.setPositiveButton(context.getString(R.string.start_collecting)) { _, _ ->
                    openNavigation(lat, lng)
                    updateReportStatus(id, "in_progress", JSONArray())
                }
            }
            "in_progress" -> {
                builder.setPositiveButton(context.getString(R.string.collected)) { _, _ ->
                    val wastes = collectWeights()
                    if (wastes != null) {
                        updateReportStatus(id, "collected", wastes)
                    }
                }
            }
        }

        builder.setNegativeButton(context.getString(R.string.close)) { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    private fun openNavigation(lat: String, lng: String) {
        if (lat != "0.0000000" && lng != "0.0000000") {
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"))
                startActivity(browserIntent)
            }
        } else {
            Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to collect entered weights and create a JSON array
    private fun collectWeights(): JSONArray? {
        val wastesArray = JSONArray()
        for (i in wasteTypesID.indices) {
            val wId = wasteTypesID[i]
            val wType = wasteTypesNames[i]
            val input = weightInputs[wType]
            val value = input?.text.toString().trim()

            if (value.isEmpty()) {
                Toast.makeText(context, "${context.getString(R.string.please_enter_weight)} $wType", Toast.LENGTH_SHORT).show()
                return null
            }

            val wasteObj = JSONObject()
            wasteObj.put("id", wId)
            try {
                wasteObj.put("weight", value.toDouble())
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid weight for $wType", Toast.LENGTH_SHORT).show()
                return null
            }
            wastesArray.put(wasteObj)
        }
        return wastesArray
    }

    // Function to update the report status
    private fun updateReportStatus(id: Int, newStatus: String, wastes: JSONArray?) {
        val url = COLLECTOR_UPDATE_STATUS_URL
        val body = JSONObject().apply {
            put("report_id", id)
            put("status", newStatus)
            put("wastes", wastes ?: JSONArray())
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, body,
            Response.Listener { response ->
                try {
                    val success = response.getBoolean(SUCCESS)
                    val message = response.getString(MESSAGE)
                    if (success) {
                        Toast.makeText(context, "${context.getString(R.string.updated)}: $message", Toast.LENGTH_SHORT).show()
                        listener?.onReportStatusChanged()
                    } else {
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton(context.getString(R.string.ok)) { d, _ -> d.dismiss() }
                            .show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, context.getString(R.string.parsing_error), Toast.LENGTH_SHORT).show()
                }
                dismiss()
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                dismiss()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = requireActivity()
                    .getSharedPreferences(DATA, AppCompatActivity.MODE_PRIVATE)
                val token = sharedPreferences.getString(TOKEN, null)
                headers[AUTHORIZATION] = "$BEARER $token"
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }
}
