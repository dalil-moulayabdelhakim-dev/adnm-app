package com.dldevalopement.adnm.home.reporter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.databinding.ItemReportBinding

// ✅ Data class الجديدة
data class Report(
    val id: Int,
    val status: String,
    val totalPrice: Double?,
    val wasteItems: List<WasteItem>,
    val date: String
)

class ReportsAdapter(
    private val context: Context,
    var reports: List<Report>,
    private val recyclerInterface: RecyclerInterface
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(
        val binding: ItemReportBinding,
        recyclerInterface: RecyclerInterface
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                recyclerInterface.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding, recyclerInterface)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        // ✅ عرض الحالة
        holder.binding.txtStatus.text = report.status

        // ✅ عرض المبلغ الإجمالي (إن وجد)
        if (report.totalPrice != null) {
            holder.binding.txtTotal.text =
                "${report.totalPrice} ${context.getString(R.string.da)}"
            holder.binding.txtTotal.visibility = View.VISIBLE
        } else {
            holder.binding.txtTotal.visibility = View.GONE
        }

        // ✅ عرض تاريخ الإنشاء
        holder.binding.txtCreatedAt.text = report.date
    }

    override fun getItemCount(): Int = reports.size

    fun updateReports(newReports: List<Report>) {
        this.reports = newReports
        notifyDataSetChanged()
    }
}
