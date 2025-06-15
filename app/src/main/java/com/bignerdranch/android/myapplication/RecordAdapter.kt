package com.bignerdranch.android.myapplication

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordAdapter(
    private var items: MutableList<WorkoutRecordEntity>,
    private val onDelete: (WorkoutRecordEntity) -> Unit
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    private val dataFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    fun updateData(newItem: List<WorkoutRecordEntity>) {
        items = newItem.toMutableList()
        notifyDataSetChanged()
    }

    class  RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mode: TextView = view.findViewById(R.id.text_mode)
        val time: TextView = view.findViewById(R.id.text_time)
        val date: TextView = view.findViewById(R.id.text_date)
        val btnDelete: Button = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]

        val sign = if (item.totalTime >= 0) "+" else "-"
        val min = item.totalTime / 1000 / 60
        val sec = (item.totalTime / 1000) % 60
        val part = (item.totalTime % 1000) / 10
        val avg = item.avgRoundTime / 1000

        val formattedTime = "총 시간: %s%02d:%02d.%02d / 평균: %02d초"
            .format(sign, min, sec, part, avg)

        val formattedDate = dataFormat.format(Date(item.timestamp))

        with(holder) {
            mode.text = "[${item.mode}] ${item.roundCount} R"
            time.text = "$formattedTime"
            date.text ="($formattedDate)"
            btnDelete.setOnClickListener { onDelete(item) }
            itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, RecordDetailActivity::class.java).apply {
                    putExtra("recordId", item.id.toLong())
                }
                context.startActivity(intent)
            }
        }
        // 필요 시 디버깅 로그
        // if (BuildConfig.DEBUG) {
        //     Log.d("Adapter", "recordId 전달: ${item.id}")
        // }
    }

    override fun getItemCount(): Int = items.size
}