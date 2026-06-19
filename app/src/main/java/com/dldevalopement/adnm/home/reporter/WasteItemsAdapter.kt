package com.dldevalopement.adnm.home.reporter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dldevalopement.adnm.R
import com.dldevalopement.adnm.databinding.ItemWasteBinding

/**
 * Adapter class for displaying a list of [WasteItem] objects inside a RecyclerView.
 *
 * This adapter binds each [WasteItem] to its corresponding view layout (ItemWasteBinding),
 * showing its type, weight, and total price.
 *
 * @property wasteItems List of waste items to be displayed in the RecyclerView.
 */
class WasteItemsAdapter(
    private var wasteItems: List<WasteItem>
) : RecyclerView.Adapter<WasteItemsAdapter.WasteViewHolder>() {

    /**
     * ViewHolder class that holds the layout binding for a single waste item.
     *
     * @property binding View binding for the waste item layout.
     */
    inner class WasteViewHolder(val binding: ItemWasteBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Inflates the layout for each waste item and returns a [WasteViewHolder].
     *
     * @param parent The parent ViewGroup in which the view will be added.
     * @param viewType The type of view (unused here as we have only one layout type).
     * @return A new instance of [WasteViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val binding = ItemWasteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WasteViewHolder(binding)
    }

    /**
     * Binds data from a [WasteItem] to its corresponding layout.
     *
     * @param holder The [WasteViewHolder] that should be updated.
     * @param position The position of the item in the adapter's list.
     */
    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val item = wasteItems[position]
        holder.binding.textWasteName.text = item.wasteType
        holder.binding.textWasteWeight.text =
            "${item.weight} ${holder.itemView.context.getString(R.string.kg)}"
        holder.binding.textWastePrice.text =
            "${item.totalPrice} ${holder.itemView.context.getString(R.string.da)}"
    }

    /**
     * Returns the number of waste items currently in the adapter.
     *
     * @return Total number of items.
     */
    override fun getItemCount(): Int = wasteItems.size

    /**
     * Updates the current list of waste items and refreshes the RecyclerView.
     *
     * @param newList The new list of [WasteItem] objects to display.
     */
    fun updateList(newList: List<WasteItem>) {
        wasteItems = newList
        notifyDataSetChanged()
    }
}
