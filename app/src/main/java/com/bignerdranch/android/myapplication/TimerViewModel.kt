package com.bignerdranch.android.myapplication

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private var startTimeMillis: Long = 0L
    private var pausedTimeMillis: Long = 0L
    private var _elapsedTime = MutableStateFlow(0L)
    private var roundStartTimeMillis: Long = 0L
    private var totalTimeMills: Long = 0L
    private val _intervalProgress = MutableStateFlow(0f)
    val intervalProgress: StateFlow<Float> = _intervalProgress
    private var intervalJob: Job? = null

    val elapsedTime: StateFlow<Long> = _elapsedTime
    private var _uiState = MutableStateFlow(TimerUIState())
    val uiState: StateFlow<TimerUIState> = _uiState



    private var isRunning = false
    private var isPaused = false
    var timeCap : Long = -1L

    fun resetRoundTimer() {
        roundStartTimeMillis = System.currentTimeMillis()
        _intervalProgress.value = 0f
    }

    fun startIntervalTracking() {
        intervalJob?.cancel()
        viewModelScope.launch {
            while (isRunning) {
                val elapsed = System.currentTimeMillis() - roundStartTimeMillis
                _intervalProgress.value = (elapsed % 60_000L).toFloat() / 60_000L
                delay(80L)
            }
        }
    }

    fun isRunning(): Boolean = isRunning

    fun setButtonState(
        start: Boolean,
        pause: Boolean,
        finish: Boolean,
        reset: Boolean,
        addRound: Boolean
    ) {
        _uiState.value = TimerUIState(
            isStartEnabled = start,
            isPauseEnabled = pause,
            isFinishEnabled = finish,
            isResetEnabled = reset,
            isAddRoundEnabled = addRound
        )
    }

    fun updateUIState(state: TimerUIState) {
        _uiState.value = state
    }

    fun start() {
        if (isRunning) return

        if (isPaused) {
            startTimeMillis = System.currentTimeMillis() - pausedTimeMillis
            roundStartTimeMillis = System.currentTimeMillis() - (pausedTimeMillis - 60_000L)
            isPaused = false
        } else {
            startTimeMillis = System.currentTimeMillis()
            pausedTimeMillis = 0L
            resetRoundTimer()
        }
        isRunning = true
        isPaused = false

        viewModelScope.launch {
            while (isRunning) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTimeMillis
                _elapsedTime.value = elapsed


                if (timeCap > 0 && elapsed >= timeCap) {
                    stop()
                }

                delay(50L)
            }
        }
    }



    fun pause() {
        if (isRunning) {
            isRunning = false
            isPaused = true
            pausedTimeMillis = _elapsedTime.value
            roundStartTimeMillis = System.currentTimeMillis() - (pausedTimeMillis % 60_000L)
        }
    }

    fun stop () {
        isRunning = false
        isPaused = false
    }

    fun reset() {
        isRunning = false
        isPaused = false
        _elapsedTime.value = 0L
        startTimeMillis = 0L
        pausedTimeMillis = 0L
        resetRoundTimer()
        intervalJob?.cancel()
    }
}