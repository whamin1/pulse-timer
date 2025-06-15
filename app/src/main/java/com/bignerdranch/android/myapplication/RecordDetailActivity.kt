package com.bignerdranch.android.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RecordDetailActivity : AppCompatActivity() {

    private lateinit var roundLogText: TextView
    private lateinit var editMemo: EditText
    private lateinit var btnSaveMemo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_detail)

        roundLogText = findViewById(R.id.text_round_log)
        editMemo = findViewById(R.id.edit_memo)
        btnSaveMemo = findViewById(R.id.btn_save_memo)

        val recordId = intent.getLongExtra("recordId", -1L)

        if (recordId == -1L) {
            roundLogText.text = "잘못된 기록입니다."
            return
        }

        val db = AppDatabase.getDatabase(applicationContext)

        lifecycleScope.launch {
            val rounds = db.workoutRoundDao().getRoundsByRecordId(recordId)

            roundLogText.text = if (rounds.isEmpty()) {
                "기록된 라운드가 없습니다."
            } else {
                rounds.joinToString("\n") { round ->
                    val abs = formatMillis(round.absoluteTime)
                    val interval = formatMillis(round.intervalTime)
                    "라운드 ${round.roundNumber} $abs ($interval)"
                }
            }
        }

        // 기존 메모 로딩
        lifecycleScope.launch {
            val record = db.workoutRecordDao().getRecordById(recordId)
            editMemo.setText(record?.memo.orEmpty())
        }

        // 메모 저장
        btnSaveMemo.setOnClickListener {
            val newMemo = editMemo.text.toString()
            lifecycleScope.launch {
                db.workoutRecordDao().updateMemo(recordId, newMemo)
                Toast.makeText(this@RecordDetailActivity, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatMillis(mills: Long): String {
        val sign = if (mills >= 0) "+" else "-"
        val absMillis = kotlin.math.abs(mills)
        val minutes = (absMillis / 1000) / 60
        val seconds = (absMillis / 1000) % 60
        val millisPart = (absMillis % 1000) / 10
        return String.format("%s%02d:%02d.%02d", sign, minutes, seconds, millisPart)
    }
}