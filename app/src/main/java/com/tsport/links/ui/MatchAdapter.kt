package com.tsport.links.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tsport.links.R
import com.tsport.links.databinding.ItemMatchBinding
import com.tsport.links.matchday.MatchStatus
import com.tsport.links.matchday.TodayMatch

class MatchAdapter(
    private val onClick: (TodayMatch) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    private val items = mutableListOf<TodayMatch>()

    fun submit(newItems: List<TodayMatch>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder =
        MatchViewHolder(
            ItemMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick
        )

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class MatchViewHolder(
        private val binding: ItemMatchBinding,
        private val onClick: (TodayMatch) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: TodayMatch) {
            val context = binding.root.context
            binding.textTime.text = matchDisplayTime(match)
            val status = when (match.status) {
                MatchStatus.FINISHED -> R.string.matchday_status_finished
                MatchStatus.LIVE -> R.string.matchday_status_live
                MatchStatus.POSTPONED -> R.string.matchday_status_postponed
                MatchStatus.CANCELLED -> R.string.matchday_status_cancelled
                else -> null
            }
            binding.textStatus.isVisible = status != null
            if (status != null) binding.textStatus.setText(status)

            binding.textHome.text = match.homeTeam
            binding.textAway.text = match.awayTeam
            binding.homeBadge.text = match.homeTeam.firstOrNull()?.uppercase().orEmpty()
            binding.awayBadge.text = match.awayTeam.firstOrNull()?.uppercase().orEmpty()

            binding.textVenue.isVisible = !match.venue.isNullOrBlank()
            binding.textVenue.text = match.venue.orEmpty()
            binding.textRound.isVisible = match.round != null
            binding.textRound.text = match.round?.let {
                context.getString(R.string.matchday_round, it)
            }.orEmpty()

            binding.root.contentDescription = context.getString(
                R.string.matchday_select,
                match.homeTeam,
                match.awayTeam,
                binding.textTime.text
            )
            binding.root.setOnClickListener { onClick(match) }
        }
    }
}
