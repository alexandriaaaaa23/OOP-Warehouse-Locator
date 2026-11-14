package com.example.oop_warehouse_location.adapters.base

import androidx.recyclerview.widget.RecyclerView

/**
 * ABSTRACTION + INHERITANCE — eliminates duplicated submitList / notifyDataSetChanged
 * boilerplate across all RecyclerView adapters.
 */
abstract class BaseListAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    protected val items = mutableListOf<T>()

    open fun submitList(newItems: List<T>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    open fun getItem(position: Int): T = items[position]

    open fun getAllItems(): List<T> = items.toList()

    override fun getItemCount(): Int = items.size
}
