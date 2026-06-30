package com.dldevalopement.adnm.home.collector.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class ReportInfoDialogFragment(
    private val context: Context,
    private val reportJson: JSONObject
) : DialogFragment() {

    interface ReportStatusListener {
        fun onReportStatusChanged()
    }

    private var listener: ReportStatusListener? = null
    private val weightInputs = mutableMapOf<String, EditText>()
    private val wasteTypesID = mutableListOf<String>()
    private val wasteTypesNames = mutableListOf<String>()

    fun setReportStatusListener(l: ReportStatusListener) {
        listener = l
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_report_info_collector, null)

        val id = reportJson.optInt("id")
        val status = reportJson.optString("status")
        val lat = reportJson.optString("latitude")
        val lng = reportJson.optString("longitude")

        // Title and Status Badge
        view.findViewById<TextView>(R.id.tvTitle).text = "${context.getString(R.string.report)} #$id"
        view.findViewById<TextView>(R.id.tvReportIdValue).text = id.toString()
        
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        tvStatusBadge.text = status.replace("_", " ").replaceFirstChar { it.uppercase() }
        
        when (status) {
            "accepted" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.blue_drive))
            "in_progress" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.orange_status))
            "collected" -> tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary_green))
        }

        // Expand/Collapse Logic
        val layoutIdHeader = view.findViewById<LinearLayout>(R.id.layoutIdHeader)
        val layoutCollapsibleInfo = view.findViewById<LinearLayout>(R.id.layoutCollapsibleInfo)
        val ivDropdownArrow = view.findViewById<ImageView>(R.id.ivDropdownArrow)
        val rootLayout = view as ViewGroup

        layoutIdHeader.setOnClickListener {
            val isVisible = layoutCollapsibleInfo.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(rootLayout)
            layoutCollapsibleInfo.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivDropdownArrow.animate().rotation(if (isVisible) 90f else 270f).start()
        }

        // Row: ID (inside dropdown)
        setupRow(view.findViewById(R.id.rowId), R.drawable.ic_hash, context.getString(R.string.id), id.toString())
        
        // Row: Status (inside dropdown)
        setupRow(view.findViewById(R.id.rowStatus), R.drawable.ic_check, context.getString(R.string.status), status.replace("_", " ").replaceFirstChar { it.uppercase() })

        // User Info (inside dropdown)
        val userObj = reportJson.optJSONObject("user")
        val userName = userObj?.let { "${it.optString("name")} ${it.optString("last_name")}" } ?: "N/A"
        val userPhone = userObj?.optString("phone_number") ?: "N/A"

        setupRow(view.findViewById(R.id.rowName), R.drawable.ic_person, context.getString(R.string.user_name), userName)
        setupRow(view.findViewById(R.id.rowPhone), R.drawable.ic_phone, context.getString(R.string.phone_number), userPhone)

        // Waste Types Names (inside dropdown)
        val containerWasteTypes = view.findViewById<LinearLayout>(R.id.containerWasteTypes)
        val wasteArray = reportJson.optJSONArray("waste_types") ?: JSONArray()
        
        // Weight Inputs Container (Outside dropdown)
        val containerWeightInputs = view.findViewById<LinearLayout>(R.id.containerWeightInputs)

        for (i in 0 until wasteArray.length()) {
            val wasteObj = wasteArray.getJSONObject(i)
            val wId = wasteObj.optString("id")
            val wType = wasteObj.optString("type", "Waste Type")
            
            wasteTypesID.add(wId)
            wasteTypesNames.add(wType)

            // Add waste type name row to dropdown
            val rowView = LayoutInflater.from(context).inflate(R.layout.item_report_info_row, containerWasteTypes, false)
            setupRow(rowView, R.drawable.ic_trash, context.getString(R.string.type), wType)
            containerWasteTypes.addView(rowView)

            // Add weight input outside dropdown if in progress
            if (status == "in_progress") {
                val weightInputView = LayoutInflater.from(context).inflate(R.layout.item_weight_input, containerWeightInputs, false)
                val tilWeight = weightInputView.findViewById<TextInputLayout>(R.id.tilWeight)
                val etWeight = weightInputView.findViewById<EditText>(R.id.etWeight)
                
                tilWeight.hint = "${context.getString(R.string.please_enter_weight)} $wType"
                weightInputs[wType] = etWeight
                containerWeightInputs.addView(weightInputView)
            }
        }

        // Buttons
        val btnDrive = view.findViewById<Button>(R.id.btnDrive)
        val btnCall = view.findViewById<Button>(R.id.btnCall)
        val btnPositive = view.findViewById<Button>(R.id.btnPositive)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        // Drive button only appears when status is in_progress
        btnDrive.visibility = if (status == "in_progress") View.VISIBLE else View.GONE
        btnDrive.setOnClickListener { openNavigation(lat, lng) }

        btnCall.setOnClickListener {
            if (userPhone != "N/A") {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$userPhone")))
            }
        }
        btnClose.setOnClickListener { dismiss() }

        // Footer Action Logic
        when (status) {
            "accepted" -> {
                btnPositive.text = context.getString(R.string.start_collecting)
                btnPositive.setOnClickListener {
                    updateReportStatus(id, "in_progress", JSONArray())
                }
            }
            "in_progress" -> {
                btnPositive.text = context.getString(R.string.collected)
                btnPositive.setOnClickListener {
                    val wastes = collectWeights()
                    if (wastes != null) {
                        updateReportStatus(id, "collected", wastes)
                    }
                }
            }
            else -> {
                btnPositive.visibility = View.GONE
            }
        }

        return AlertDialog.Builder(context, R.style.dialog)
            .setView(view)
            .create()
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
            Toast.makeText(context, context.getString(R.string.not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectWeights(): JSONArray? {
        val wastesArray = JSONArray()
        for (i in wasteTypesID.indices) {
            val wType = wasteTypesNames[i]
            val value = weightInputs[wType]?.text.toString().trim()
            if (value.isEmpty()) {
                Toast.makeText(context, "${context.getString(R.string.please_enter_weight)} $wType", Toast.LENGTH_SHORT).show()
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
                    listener?.onReportStatusChanged()
                    dismiss()
                } else {
                    Toast.makeText(context, response.optString(MESSAGE), Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { dismiss() }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val token = context.getSharedPreferences(DATA, Context.MODE_PRIVATE).getString(TOKEN, "")
                return mutableMapOf(AUTHORIZATION to "$BEARER $token", "Accept" to "application/json")
            }
        }
        Volley.newRequestQueue(context).add(request)
    }
}
