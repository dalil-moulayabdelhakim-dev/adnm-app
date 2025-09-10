package com.dldevalopement.adnm.home.reporter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.databinding.ItemWasteBinding

class WasteItemsAdapter(
    private var wasteItems: List<WasteItem>
) : RecyclerView.Adapter<WasteItemsAdapter.WasteViewHolder>() {

    inner class WasteViewHolder(val binding: ItemWasteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val binding = ItemWasteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WasteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val item = wasteItems[position]
        holder.binding.textWasteName.text = item.wasteType
        holder.binding.textWasteWeight.text =
            "${item.weight} ${holder.itemView.context.getString(R.string.kg)}"
        holder.binding.textWastePrice.text =
            "${item.totalPrice} ${holder.itemView.context.getString(R.string.da)}"
    }

    override fun getItemCount(): Int = wasteItems.size

    fun updateList(newList: List<WasteItem>) {
        wasteItems = newList
        notifyDataSetChanged()
    }
}
