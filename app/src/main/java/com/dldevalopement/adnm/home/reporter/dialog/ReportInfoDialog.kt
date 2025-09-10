package com.dldevalopement.adnm.home.reporter.dialog

// Import necessary Android, Volley, and local classes
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.database.*
import com.dldevalopement.adnm.databinding.DialogReportInfoBinding
import com.dldevalopement.adnm.home.reporter.Report
import com.dldevalopement.adnm.home.reporter.WasteItem
import com.dldevalopement.adnm.home.reporter.WasteItemsAdapter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * A DialogFragment to display detailed information about a single waste report.
 * It fetches the report data from the server and displays it in a dialog.
 *
 * @param context The context of the calling activity.
 * @param reportId The ID of the report to display.
 */
class ReportInfoDialog(private val context: Context, private val reportId: Int) : DialogFragment() {

    // View binding instance for safe access to views
    private lateinit var _binding: DialogReportInfoBinding
    private val binding get() = _binding

    /**
     * Creates and configures the AlertDialog for displaying report information.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for this dialog fragment
        _binding = DialogReportInfoBinding.inflate(layoutInflater)

        // Fetch the report data from the server
        fetchReportById(reportId)



        // Build and return the AlertDialog
        return AlertDialog.Builder(context, R.style.dialog)
            .setTitle(context.getString(R.string.report_information))
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                dismiss()
            }
            .create()
    }

    /**
     * Fetches a single report from the server using its ID.
     * @param reportId The ID of the report to fetch.
     */
    private fun fetchReportById(reportId: Int) {
        val request = object : StringRequest(
            Method.GET,
            "$REPORTER_SHOW_REPORT_URL/$reportId",
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val success = jsonObject.getBoolean(SUCCESS)
                    if (success) {
                        val obj = jsonObject.getJSONObject(REPORT)

                        val status = obj.getString(STATUS)
                        val totalPrice = if (obj.has("total_price") && !obj.isNull("total_price")) {
                            obj.getDouble("total_price")
                        } else null
                        val createdAt = formatDate(obj.getString("created_at"))

                        // Parse waste_items
                        val wasteItemsArray = obj.getJSONArray("waste_items")
                        val wasteItems = mutableListOf<WasteItem>()
                        for (i in 0 until wasteItemsArray.length()) {
                            val w = wasteItemsArray.getJSONObject(i)
                            wasteItems.add(
                                WasteItem(
                                    wasteType = w.getString("waste_type")?: "",
                                    weight = if (w.isNull("weight")) 0.0 else w.optDouble("weight", 0.0),
                                    totalPrice = if (w.isNull("total_price")) 0.0 else w.optDouble("total_price", 0.0)
                                )
                            )
                        }

                        val report = Report(
                            id = obj.getInt("id"),
                            status = status,
                            totalPrice = totalPrice,
                            wasteItems = wasteItems,
                            date = createdAt
                        )

                        // عرض البيانات في الـ Dialog
                        binding.textStatus.text = report.status
                        binding.textCreationDate.text = report.date
                        binding.textTotalPrice.text =
                            if (report.totalPrice != null) "${report.totalPrice} ${context.getString(R.string.da)}"
                            else context.getString(R.string.not_available)

                        // إنشاء Adapter للنفايات
                        val adapter = WasteItemsAdapter(report.wasteItems)
                        binding.recyclerWasteItems.layoutManager = LinearLayoutManager(context)
                        binding.recyclerWasteItems.adapter = adapter

                    } else {
                        val message = jsonObject.getString(MESSAGE)
                        val dialog = AlertDialog.Builder(context)
                            .setMessage(message)
                            .setPositiveButton(context.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .create()
                        dialog.show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        context,
                        context.getString(R.string.parsing_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, { error ->
                error.printStackTrace()
                dismiss()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = context.getSharedPreferences(DATA, Context.MODE_PRIVATE)
                headers[AUTHORIZATION] = "$BEARER ${sharedPreferences.getString(TOKEN, null)}"
                return headers
            }
        }
        Volley.newRequestQueue(context).add(request)
    }

    /**
     * Formats an ISO 8601 date string into a more readable format.
     * @param isoString The date string in ISO 8601 format.
     * @return The formatted date string. Returns the original string if parsing fails.
     */
    fun formatDate(isoString: String): String {
        return try {
            // Define the input and output date formats
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

            // Parse the ISO string and format it to the desired output
            val date = isoFormat.parse(isoString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            // Log the error and return the original string if something goes wrong
            e.printStackTrace()
            isoString
        }
    }
}