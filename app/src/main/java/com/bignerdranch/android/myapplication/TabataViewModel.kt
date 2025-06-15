package com.bignerdranch.android.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlin.time.Duration

class TabataViewModel : ViewModel() {

    enum class Phase { IDLE, WORK, REST, DONE}

    private val _remainTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainTime

    private val _statusText = MutableStateFlow("")
    val statusText : StateFlow<String> = _statusText

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase

    val isPaused = MutableStateFlow(false)
    private var totalSets: Int = 8
    private var workTime: Long = 20_000L
    private var restTime: Long = 10_000L

    private var isRunning = false
    private var timerJob: Job? = null
    private var pausedElapsed: Long = 0L
    private var startTimestamp: Long = 0L
    private var started = false
    private var configured = false
    private var pausedAt = 0L
    private var totalPausedTime = 0L
    private var roundPauseAt = 0L


    private var currentPhaseDuration: Long = 0L
    private var PhaseStartTimestamp: Long = 0L

    fun isFirstStart(): Boolean = !started
    fun getTotalSets(): Int = totalSets
    fun phaseStartTime(): Long = PhaseStartTimestamp
    fun isRunning(): Boolean = isRunning
    fun setStatusText(text: String) {
        _statusText.value = text
    }

    fun configure(work: Long, rest: Long, sets: Int) {
        if (configured) return

        configured = true
        _statusText.value = "준비하세요"
        workTime = work
        restTime = rest
        totalSets = sets
        _phase.value = Phase.IDLE
        _remainTime.value = workTime
    }

    fun start() {
        started = true
        isPaused.value = false

        if (_phase.value == Phase.IDLE) {
            startWork()
            return
        }

        if (_phase.value == Phase.DONE) return

        isRunning = true
        startTimestamp = System.currentTimeMillis() - pausedElapsed

        launchTimer()
    }

    fun pause() {
        if (!isRunning) return

        isPaused.value = true
        isRunning = false
        pausedAt = System.currentTimeMillis()
        roundPauseAt = System.currentTimeMillis()
        timerJob?.cancel()
        pausedElapsed = pausedAt - startTimestamp
        timerJob?.cancel()
    }

    fun resume() {
        if (isRunning || _phase.value == Phase.DONE) return

        val now = System.currentTimeMillis()
        val pauseDuration = now - pausedAt

        totalPausedTime += pauseDuration
        startTimestamp += pauseDuration
        PhaseStartTimestamp += pauseDuration


        isRunning = true
        isPaused.value = false

        launchTimer()
    }

    fun reset() {
        isRunning = false
        started = false
        configured = false
        isPaused.value = false

        timerJob?.cancel()
        pausedElapsed = 0L
        startTimestamp = 0L
        _currentSet.value = 1
        _phase.value = Phase.IDLE
        _remainTime.value = workTime
    }

    private fun finish() {
        isRunning = false
        timerJob?.cancel()
        _remainTime.value = 0L
        _currentSet.value = 1
        _phase.value = Phase.DONE
    }

    private fun startWork() {
        _phase.value = Phase.WORK
        currentPhaseDuration = workTime
        pausedElapsed = 0L
        PhaseStartTimestamp = System.currentTimeMillis()
        start()

    }
    private fun startRest() {
        _phase.value = Phase.REST
        currentPhaseDuration = restTime
        pausedElapsed = 0L
        PhaseStartTimestamp = System.currentTimeMillis()
        start()
    }

    private fun launchTimer() {
        currentPhaseDuration = when (_phase.value) {
            Phase.WORK -> workTime
            Phase.REST -> restTime
            else -> 0L
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isRunning) {
                val elapsed = System.currentTimeMillis() - startTimestamp
                val remaining = (currentPhaseDuration - elapsed).coerceAtLeast(0L)
                _remainTime.value = remaining

                if (remaining <= 0) {
                    when (_phase.value) {
                        Phase.WORK -> {
                            if (_currentSet.value < totalSets) {
                                startRest()
                            } else {
                                finish()
                            }
                        }
                        Phase.REST -> {
                            _currentSet.value += 1
                            if (_currentSet.value > totalSets) {
                                finish()
                            } else {
                                startWork()
                            }
                        }
                        else -> {}
                    }
                    break
                }

                delay(100L)
            }
        }
    }

}

