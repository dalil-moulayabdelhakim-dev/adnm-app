package com.dldevalopement.adnm.home.collector.dialog

// Import necessary Android and Volley classes
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.fragment.app.DialogFragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.AUTHORIZATION
import com.dldevalopement.adnm.database.BEARER
import com.dldevalopement.adnm.database.COLLECTOR_UPDATE_STATUS_URL
import com.dldevalopement.adnm.database.DATA
import com.dldevalopement.adnm.database.MESSAGE
import com.dldevalopement.adnm.database.REPORT_ID
import com.dldevalopement.adnm.database.STATUS
import com.dldevalopement.adnm.database.SUCCESS
import com.dldevalopement.adnm.database.TOKEN
import org.json.JSONArray
import org.json.JSONObject

/**
 * A DialogFragment that displays a report's details to a 'Collector' user.
 * It allows the user to update the report's status based on its current state.
 *
 * @param context The context of the calling activity.
 * @param id The ID of the report.
 * @param weight The weight of the waste reported.
 * @param status The current status of the report (e.g., 'accepted', 'in_progress').
 */
class ReportInfoDialogFragment(
    private val context: Context,
    private val id: Int,
    private val wasteTypesID: List<String>,
    private val wasteTypes: List<String>, // الأنواع المرتبطة بالبلاغ
    private val status: String
) : DialogFragment() {

    /**
     * An interface to define a callback for when the report's status is changed.
     * This allows the calling activity to refresh its UI after the update.
     */
    interface ReportStatusListener {
        fun onReportStatusChanged()
    }

    private var listener: ReportStatusListener? = null
    private val weightInputs = mutableMapOf<String, EditText>()

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

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_report_info_collector, null)
        val container = view.findViewById<LinearLayout>(R.id.containerWasteTypes)
        view.findViewById<TextView>(R.id.tvId).text = "${context.getString(R.string.id)}: $id"
        view.findViewById<TextView>(R.id.tvStatus).text = "${context.getString(R.string.status)}: $status"


        // إضافة الحقول ديناميكيا
        wasteTypes.forEach { type ->
            val input = EditText(context).apply {
                setBackgroundResource(R.drawable.shape_feilds)
                hint = type
            }

// تعيين margin (بالـ dp)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16) // left, top, right, bottom (16px, حولها dp تحت)
            input.layoutParams = params
            container.addView(input)

            weightInputs[type] = input
        }

        builder.setView(view)
        builder.setCancelable(false)
        builder.setTitle("${context.getString(R.string.report)} #$id")

        when (status) {
            "accepted" -> {
                builder.setPositiveButton(context.getString(R.string.start_collecting)) { _, _ ->
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

    // Function to collect entered weights and create a JSON array
    private fun collectWeights(): JSONArray? {
        val wastesArray = JSONArray()

        // Loop through each waste type
        for (i in wasteTypesID.indices) {
            val id = wasteTypesID[i]
            val type = wasteTypes[i]
            val input = weightInputs[type]
            val value = input?.text.toString().trim()

            // Check if weight input is empty
            if (value.isEmpty()) {
                Toast.makeText(context, "${context.getString(R.string.please_enter_weight)} $type", Toast.LENGTH_SHORT).show()
                return null
            }

            // Create a JSON object for the current waste
            val wasteObj = JSONObject()
            wasteObj.put("id", id)
            wasteObj.put("weight", value.toDouble())
            wastesArray.put(wasteObj)
        }

        // Return the final array of wastes
        return wastesArray
    }

    // Function to update the report status with new status and waste data
    private fun updateReportStatus(id: Int, newStatus: String, wastes: JSONArray?) {
        val url = COLLECTOR_UPDATE_STATUS_URL

        // Create the request body
        val body = JSONObject().apply {
            put("report_id", id)
            put("status", newStatus)
            put("wastes", wastes ?: JSONArray())
        }

        // Create a POST request using Volley
        val request = object : JsonObjectRequest(
            Method.POST, url, body,
            Response.Listener { response ->
                try {
                    // Parse the response
                    val success = response.getBoolean(SUCCESS)
                    val message = response.getString(MESSAGE)

                    if (success) {
                        // If success, show confirmation and notify listener
                        Toast.makeText(context, "${context.getString(R.string.updated)}: $message", Toast.LENGTH_SHORT).show()
                        listener?.onReportStatusChanged()
                    } else {
                        // If failed, show alert dialog with the error message
                        AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton(context.getString(R.string.ok)) { d, _ -> d.dismiss() }
                            .show()
                    }
                } catch (e: Exception) {
                    // Handle parsing exceptions
                    e.printStackTrace()
                    Toast.makeText(context, context.getString(R.string.parsing_error), Toast.LENGTH_SHORT).show()
                }
                dismiss()
            },
            Response.ErrorListener { error ->
                // Handle request errors
                error.printStackTrace()
                dismiss()
            }
        ) {
            // Add headers (including authentication token)
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

        // Add request to the queue
        Volley.newRequestQueue(requireContext()).add(request)
    }



}