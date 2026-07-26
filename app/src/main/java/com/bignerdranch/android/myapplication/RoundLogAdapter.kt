package com.bignerdranch.android.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoundLogAdapter(
    private var roundList: List<RoundEntry>,
    private val timerMode: TimerMode,
    private val roundViewModel: RoundViewModel
) : RecyclerView.Adapter<RoundLogAdapter.RoundViewHolder>(){

    class RoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roundTextView: TextView = itemView.findViewById(R.id.text_round_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoundViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_round_log, parent, false)
        return RoundViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoundViewHolder, position: Int) {
        val entry = roundList[position]

        val abs = if (timerMode == TimerMode.FOR_TIME || timerMode == TimerMode.PULSE) {
            roundViewModel.formatMillisWithSign(entry.absoluteTime)
        } else {
            roundViewModel.formatMillis(entry.absoluteTime)
        }

        val interval = if (timerMode == TimerMode.FOR_TIME || timerMode == TimerMode.PULSE) {
            roundViewModel.formatMillisWithSign(entry.intervalTime)
        } else {
            roundViewModel.formatMillisWithSign(entry.intervalTime)
        }

        holder.roundTextView.text = "라운드 ${entry.roundNumber}: $abs ($interval)"
    }

    override fun getItemCount(): Int = roundList.size

    fun updateList(newList: List<RoundEntry>) {
        roundList = newList
        notifyDataSetChanged()
    }
}
