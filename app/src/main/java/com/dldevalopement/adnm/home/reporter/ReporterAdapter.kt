package com.dldevalopement.adnm.home.reporter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.databinding.ItemReportBinding

// ✅ Data class that represents a single report item
data class Report(
    val id: Int,                // Unique report ID
    val status: String,         // Current status of the report
    val totalPrice: Double?,    // Optional total price for collected waste
    val wasteItems: List<WasteItem>, // List of waste items in this report
    val date: String            // Creation date of the report
)

// Adapter for displaying reports in a RecyclerView
class ReportsAdapter(
    private val context: Context,
    var reports: List<Report>,
    private val recyclerInterface: RecyclerInterface
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    private var highlightedReportId: Int? = null

    // ViewHolder that holds the layout binding for each report item
    inner class ReportViewHolder(
        val binding: ItemReportBinding,
        recyclerInterface: RecyclerInterface
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Handle item click event
            binding.root.setOnClickListener {
                recyclerInterface.onItemClick(adapterPosition)
            }
        }
    }

    // Inflate the item layout and create the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding, recyclerInterface)
    }

    // Bind report data to the item layout
    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        // ✅ Highlight logic
        if (report.id == highlightedReportId) {
            holder.binding.root.setCardBackgroundColor(context.getColor(R.color.lightGreen))
            holder.binding.root.strokeColor = context.getColor(R.color.primary_green)
        } else {
            holder.binding.root.setCardBackgroundColor(context.getColor(R.color.white))
            holder.binding.root.strokeColor = context.getColor(R.color.border_color)
        }

        // ✅ Display report status
        holder.binding.txtStatus.text = report.status

        // ✅ Display total price if available, otherwise hide the field
        if (report.totalPrice != null) {
            holder.binding.txtTotal.text =
                "${report.totalPrice} ${context.getString(R.string.da)}"
            holder.binding.txtTotal.visibility = View.VISIBLE
        } else {
            holder.binding.txtTotal.visibility = View.GONE
        }

        // ✅ Display creation date
        holder.binding.txtCreatedAt.text = report.date
    }

    // Return the total number of report items
    override fun getItemCount(): Int = reports.size

    // Update the list of reports and refresh the RecyclerView
    fun updateReports(newReports: List<Report>, highlightId: Int? = null) {
        this.reports = newReports
        this.highlightedReportId = highlightId
        notifyDataSetChanged()

        if (highlightId != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                this.highlightedReportId = null
                // Notify specific item if possible for better performance, 
                // but notifyDataSetChanged is safe here given the context
                notifyDataSetChanged()
            }, 2000)
        }
    }
}
