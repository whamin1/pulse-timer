package com.bignerdranch.android.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoundEntry(val roundNumber: Int, val absoluteTime: Long, val intervalTime: Long)

class RoundViewModel : ViewModel() {

    private val _rounds = MutableStateFlow<List<RoundEntry>>(emptyList())
    val rounds: StateFlow<List<RoundEntry>> = _rounds.asStateFlow()

    private val _roundLogs = MutableStateFlow<List<String>>(emptyList())
    val roundLogs: StateFlow<List<String>> = _roundLogs.asStateFlow()

    private val _roundCount = MutableStateFlow(0)
    val roundCount: StateFlow<Int> = _roundCount

    private var startTime: Long = 0L
    var lastTime: Long = 0L
        private set

    private var totalTime: Long = 0L

    fun setTotalTime(time: Long) {
        totalTime = time
    }

    fun setStartTime(baseElapsedTime: Long) {
        startTime = baseElapsedTime
        lastTime = baseElapsedTime
    }

    fun addRound(currentRemainingTime: Long, timerMode: TimerMode, customTotalTime: Long = 0L) {
        val total = if (timerMode == TimerMode.AMRAP) customTotalTime else totalTime

        val absolute = when (timerMode) {
            TimerMode.AMRAP -> total - currentRemainingTime
            else -> currentRemainingTime
        }

        val interval = when (timerMode) {
            TimerMode.PULSE, TimerMode.FOR_TIME -> {
                if (_rounds.value.isEmpty()) absolute
                else absolute - _rounds.value.last().absoluteTime
            }

            TimerMode.AMRAP -> {
                if (_rounds.value.isEmpty()) totalTime - absolute
                else _rounds.value.last().absoluteTime - absolute
            }
            else -> 0L
        }


        val roundNumber = _rounds.value.size + 1
        val entry = RoundEntry(roundNumber, absolute, interval)
        _roundCount.value = roundNumber
        _rounds.value = _rounds.value + entry

        _roundLogs.value = _rounds.value.map {
            val abs = formatMillis(it.absoluteTime)
            val int = formatMillisWithSign(it.intervalTime)
            "라운드 ${it.roundNumber} $abs ($int)"
        }
        lastTime = absolute
    }


    fun reset() {
        _rounds.value = emptyList()
        _roundLogs.value = emptyList()
        _roundCount.value = 0
        startTime = 0L
        lastTime = startTime
    }




    fun formatMillis(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val minutes = (safeMillis / 1000) / 60
        val seconds = (safeMillis / 1000) % 60
        val millisPart = safeMillis % 1000 / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, millisPart)
    }

    fun formatMillisWithSign(millis: Long): String {
        val sign = if (millis >= 0) "+" else "-"
        val absMillis = kotlin.math.abs(millis)
        val minutes = (absMillis / 1000) / 60
        val seconds = (absMillis / 1000) % 60
        val millisPart = absMillis % 1000 / 10
        return String.format("%s%02d:%02d.%02d", sign, minutes, seconds, millisPart)
    }
}