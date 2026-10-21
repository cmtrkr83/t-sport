package com.tsport.links.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tsport.links.databinding.ItemLinkBinding
import com.tsport.links.links.LinkItem

class LinkAdapter(
    private val onClick: (LinkItem) -> Unit
) : RecyclerView.Adapter<LinkAdapter.LinkViewHolder>() {

    private val items = mutableListOf<LinkItem>()

    fun submit(newItems: List<LinkItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val binding = ItemLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        holder.bind(position + 1, items[position])
    }

    override fun getItemCount(): Int = items.size

    class LinkViewHolder(
        private val binding: ItemLinkBinding,
        private val onClick: (LinkItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(number: Int, item: LinkItem) {
            binding.textNumber.text = number.toString()
            binding.textDomain.text = item.author ?: item.domain
            binding.textUrl.text = item.url
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
