package com.bignerdranch.android.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AmrapViewModel: ViewModel() {

    //상태
    private var isConfigured = false
    private var isPaused = false
    private var started = false
    private var isRunning = false
    private var isRoundRunning = false

    //시관 관련
    private var totalTime: Long = 300_000L
    private var startTimestamp: Long = 0L
    private var pausedElapsed: Long = 0L
    private var startTime = 0L
    private var pausedAt = 0L
    private var roundPausedAt = 0L
    private var roundElapsedWhenPaused = 0L
    private var roundStartTime: Long = 0L

    //job
    private var timerJob: Job? = null
    private var roundTimerJob: Job? = null

    //상태 플로우
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _totalProgress = MutableStateFlow(0f)
    val totalProgress: StateFlow<Float> = _totalProgress

    private val _intervalProgress = MutableStateFlow(0f)
    val intervalProgress: StateFlow<Float> = _intervalProgress

    //외부 접근용
    fun getRoundStartTime(): Long = roundStartTime
    fun isRunning(): Boolean = isRunning
    fun isFirstStart(): Boolean = !started
    fun setStatusText(text: String) {
        _statusText.value = text
    }

    fun configure(totalMills: Long) {
        if (isConfigured) return

        totalTime = totalMills
        _remainingTime.value = totalMills
        startTimestamp = 0L
        pausedElapsed = 0L
        isRunning = false
        isConfigured = true

        _isFinished.value = false
    }

    private fun launchTimerJob() {
        timerJob = viewModelScope.launch {
            while (isRunning) {
                val elapsed = System.currentTimeMillis() - startTimestamp
                val remaining = (totalTime - elapsed).coerceAtLeast(0L)

                _remainingTime.value = remaining
                _totalProgress.value = (elapsed.toFloat() / totalTime).coerceIn(0f, 1f)

                if (remaining <= 0) {
                    finish()
                    break
                }
                delay(100L)
            }
        }
    }

    private fun launchRoundTimerJob() {
        roundTimerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - roundStartTime
                _intervalProgress.value = (elapsed.toFloat() % 60_000f) / 60_000f
                delay(80L)
            }
        }
    }

    fun startRoundTimer() {
        isRoundRunning = true
        roundTimerJob?.cancel()
        roundStartTime = System.currentTimeMillis()
        launchRoundTimerJob()
    }

    fun start() {
        started = true
        if (isRunning) {

            return
        }
        isRunning = true

        if (isPaused) {
            val pauseDuration = System.currentTimeMillis() - pausedAt
            startTime += pauseDuration
            startTimestamp += pauseDuration
            isPaused = false
        } else {
            startTime = System.currentTimeMillis()
            startTimestamp = startTime
            startRoundTimer()
        }

        launchTimerJob()
    }

    private fun finish() {
        _isFinished.value = true
        roundTimerJob?.cancel()
        timerJob?.cancel()
        isRunning = false
    }

    fun pause() {
       if (!isRunning) return

        isRunning = false
        isPaused = true
        pausedAt = System.currentTimeMillis()
        timerJob?.cancel()
        roundPausedAt = pausedAt

        roundTimerJob?.cancel()
    }

    fun resume() {
        if (!isPaused) return

        val now = System.currentTimeMillis()
        val pauseDuration = now - pausedAt
        val roundPauseDuration = now - roundPausedAt

        startTimestamp += pauseDuration

        isRunning = true
        isPaused = false

        if (isRoundRunning) {
            roundStartTime += now - roundPausedAt

            roundTimerJob = viewModelScope.launch {
                while (isRunning) {
                    val elapsed = System.currentTimeMillis() - roundStartTime
                    _intervalProgress.value = (elapsed.toFloat() % 60_000f) / 60_000f
                    delay(80L)
                }
            }
        }

        launchTimerJob()
    }

    fun reset() {
        _remainingTime.value = totalTime
        _totalProgress.value = 0f
        _intervalProgress.value = 0f
        _isFinished.value = false

        isRunning = false
        isPaused = false
        started = false
        isConfigured = false
        isRoundRunning = false

        timerJob?.cancel()
        roundTimerJob?.cancel()

        pausedElapsed = 0L
        roundElapsedWhenPaused = 0L
        startTimestamp = 0L
        roundStartTime = 0L
    }

}