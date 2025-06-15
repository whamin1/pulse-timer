package com.bignerdranch.android.myapplication

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class RecordActivity : AppCompatActivity() {

    private lateinit var adapter : RecordAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        val db = AppDatabase.getDatabase(applicationContext)
        val textEmpty = findViewById<TextView>(R.id.text_empty)
        val recyclerView = findViewById<RecyclerView>(R.id.record_recycler)

        adapter = RecordAdapter(mutableListOf()) { toDelete ->
            lifecycleScope.launch {
               db.workoutRecordDao().delete(toDelete)
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecordActivity)
            adapter = this@RecordActivity.adapter
        }

        // 실시간 Flow 수집(중복 방지 위해 LoadRecords() 제거)
        lifecycleScope.launch {
            db.workoutRecordDao().getAllRecordFlow().collectLatest { records ->
                adapter.updateData(records)
                textEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}


