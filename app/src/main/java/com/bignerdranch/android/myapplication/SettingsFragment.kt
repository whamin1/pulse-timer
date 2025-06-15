package com.bignerdranch.android.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var textWorkTime: TextView
    private lateinit var textRestTime: TextView
    private lateinit var textSets: TextView
    private lateinit var btnConfirm: ImageButton

    private lateinit var timerMode: TimerMode
    private var totalSets = 8
    private var workTime = 20_000L
    private var restTime = 10_000L
    private var forTimeCapMillis: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val prefs = requireContext().getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

        val modeString = arguments?.getString("mode") ?: prefs.getString("last_mode", "TABATA")
        timerMode = TimerMode.valueOf(modeString ?: "TABATA")

        val layoutTimeCap = view.findViewById<LinearLayout>(R.id.layout_timecap)
        val btnTimeCap = view.findViewById<TextView>(R.id.btn_set_timecap)
        val btnNone = view.findViewById<TextView>(R.id.btn_set_none)

        layoutTimeCap.visibility = if (timerMode == TimerMode.FOR_TIME) View.VISIBLE else View.GONE

        textWorkTime = view.findViewById(R.id.text_work_time)
        textRestTime = view.findViewById(R.id.text_rest_time)
        textSets = view.findViewById(R.id.text_set)
        btnConfirm = view.findViewById(R.id.btn_confirm)

        // 불러오기
        workTime = prefs.getLong("${timerMode.name}_workTime", 20_000L)
        restTime = prefs.getLong("${timerMode.name}_restTime", 10_000L)
        totalSets = prefs.getInt("${timerMode.name}_sets", 8)
        forTimeCapMillis = prefs.getLong("${timerMode.name}_forTimeCap", -1L)

        Log.d("SettingsFragment", "불러온 forTimeCapMillis: $forTimeCapMillis")
        if (forTimeCapMillis > 0) {
            btnTimeCap.setSingleLine(false)
            btnTimeCap.text = formatTimeText("",forTimeCapMillis)
        } else {
            btnTimeCap.text = "None"
        }

        setupUIForMode(timerMode)

        textWorkTime.setOnClickListener {
            showNumberPickerBottomSheet("운동 시간 (초)", workTime) {
                workTime = it
                textWorkTime.text = formatTimeText("운동", it)

            }
        }

        textRestTime.setOnClickListener {
            showNumberPickerBottomSheet("휴식 시간 (초)", restTime) {
                restTime = it

                textRestTime.text = formatTimeText("휴식", it)
            }
        }

        textSets.setOnClickListener {
            showNumberPickerBottomSheet("세트 수", totalSets) {
                totalSets = it
                textSets.text = "세트: $it"
            }
        }

        btnTimeCap.setOnClickListener {
            showNumberPickerBottomSheet("운동 시간", 0L) { selectedMillis ->
                forTimeCapMillis = selectedMillis
                btnTimeCap.text = formatTimeText("", selectedMillis)
            }
        }

        btnNone.setOnClickListener {
            forTimeCapMillis = -1L
            btnTimeCap.text = "None"
            Log.d("SettingsFragment", "타임캡 NONE으로 초기화함")
        }

        btnConfirm.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putLong("${timerMode.name}_workTime", workTime)
                putLong("${timerMode.name}_restTime", restTime)
                putInt("${timerMode.name}_sets", totalSets)
                putLong("${timerMode.name}_forTimeCap", forTimeCapMillis)
                putString("last_mode", timerMode.name)
                apply()
            }

            val intent = Intent(requireContext(), TimerActivity::class.java).apply {
                putExtra("mode", timerMode.name)
                putExtra("workTime", workTime)
                putExtra("restTime", restTime)
                putExtra("sets", totalSets)
                putExtra("forTimeCap", forTimeCapMillis)
            }
            startActivity(intent)
        }

        return view
    }

    private fun setupUIForMode(mode: TimerMode) {
        when (mode) {
            TimerMode.TABATA -> {
                textWorkTime.visibility = View.VISIBLE
                textRestTime.visibility = View.VISIBLE
                textSets.visibility = View.VISIBLE
            }
            TimerMode.EMOM -> {
                textWorkTime.visibility = View.VISIBLE
                textRestTime.visibility = View.GONE
                textSets.visibility = View.VISIBLE

            }
            TimerMode.AMRAP -> {
                textWorkTime.visibility = View.VISIBLE
                textRestTime.visibility = View.GONE
                textSets.visibility = View.GONE

            }
            TimerMode.FOR_TIME -> {
                textWorkTime.visibility = View.GONE
                textRestTime.visibility = View.GONE
                textSets.visibility = View.GONE
            }
            TimerMode.PULSE -> {
                textWorkTime.visibility = View.GONE
                textRestTime.visibility = View.GONE
                textSets.visibility = View.GONE
            }

        }

        textWorkTime.text = formatTimeText("운동", workTime)
        textRestTime.text = formatTimeText("휴식", restTime)
        textSets.text = "세트: $totalSets"
    }


    private fun showNumberPickerBottomSheet(
        title: String,
        initialMillis: Long,
        onSet: (Long) -> Unit
    ) {
        val bottomSheetDialog = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_number_picker, null)

        val titleText = view.findViewById<TextView>(R.id.dialog_title)
        val minPicker = view.findViewById<NumberPicker>(R.id.min_picker)
        val secPicker = view.findViewById<NumberPicker>(R.id.sec_picker)
        val confirmBtn = view.findViewById<Button>(R.id.btn_confirm1)

        titleText.text = title

        val initialTotalSeconds = (initialMillis / 1000).toInt()
        val initialMin = initialTotalSeconds / 60
        val initialSec = initialTotalSeconds % 60
        val secondValues = (0..55 step 5).toList()

        minPicker.minValue = 0
        minPicker.maxValue = 59
        minPicker.value = initialMin

        secPicker.minValue = 0
        secPicker.maxValue = secondValues.size - 1
        secPicker.displayedValues = secondValues.map { String.format("%02d", it) }.toTypedArray()
        secPicker.value = secondValues.indexOf(initialSec - (initialSec % 5))

        val dialog = bottomSheetDialog.setView(view).create()

        confirmBtn.setOnClickListener {
            val selectedSec = secondValues[secPicker.value]
            val totalSeconds = minPicker.value * 60 + selectedSec
            onSet(totalSeconds * 1000L)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNumberPickerBottomSheet(
        title: String,
        initialValue: Int,
        minValue: Int = 1,
        maxValue: Int = 60,
        onSet: (Int) -> Unit
    ) {
        val bottomSheetDialog = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_set_picker, null)

        val titleText = view.findViewById<TextView>(R.id.dialog_title)
        val setPicker = view.findViewById<NumberPicker>(R.id.set_picker)
        val confirmBtn = view.findViewById<Button>(R.id.btn_confirm2)

        titleText.text = title

        setPicker.minValue = minValue
        setPicker.maxValue = maxValue
        setPicker.value = initialValue

        val dialog = bottomSheetDialog.setView(view).create()

        confirmBtn.setOnClickListener {
            onSet(setPicker.value)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatTimeText(label: String, timeMillis: Long): String {
        val minutes = (timeMillis / 1000 / 60).toInt()
        val seconds = (timeMillis / 1000 % 60).toInt()

        return when {
            minutes > 0 && seconds > 0 -> "$label: ${minutes}분 ${seconds}초"
            minutes > 0 -> "$label: ${minutes}분"
            else -> "$label: ${seconds}초"
        }
    }

    companion object {
        fun newInstance(mode: TimerMode): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            args.putString("mode", mode.name)
            fragment.arguments = args
            return fragment
        }
    }
}