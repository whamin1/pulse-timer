package com.bignerdranch.android.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PulseViewModel : ViewModel() {

    //상태 Flow
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime
    private val _intervalProgress = MutableStateFlow(0f)
    val intervalProgress: StateFlow<Float> = _intervalProgress

    // 내부 상태
    private var isRunning = false
    private var isPaused = false
    private var pauseTime: Long = 0L
    private var roundStartTimeMillis: Long = 0L

    // Job 관리
    private var pulseJob: Job? = null
    private var countUpJob: Job? = null
    private var intervalJob: Job? = null

    fun isRunning(): Boolean = isRunning

    /** 새로운 라운드 시작 */
    fun startNewRound(onPulse: () -> Unit, fromPausedState: Boolean = false) {
        isRunning = true
        isPaused = false

        // Jon 정리
        countUpJob?.cancel()
        intervalJob?.cancel()
        pulseJob?.cancel()

        if (!fromPausedState) {
            roundStartTimeMillis = System.currentTimeMillis()
            _elapsedTime.value = 0L
            pauseTime = 0L
        }

        startIntervalTracking()

        // 초 단위 증가 타이머
        countUpJob = viewModelScope.launch {
            while (isRunning) {
                delay(40L)
                _elapsedTime.value +=40L
            }
        }

        // 30초 후 콜 백 트리거
        pulseJob = viewModelScope.launch {
            val delayLeft = 30_000L - pauseTime
            delay(delayLeft.coerceAtLeast(0L))
            pauseTime = 0L
            onPulse()
        }
    }

    /** 인터벌 초침 트래킹 */
    fun startIntervalTracking() {
        intervalJob?.cancel()
        viewModelScope.launch {
            while (isRunning) {
                val elapsed = System.currentTimeMillis() - roundStartTimeMillis
                _intervalProgress.value = (elapsed % 30_000L).toFloat() / 30_000L
                delay(100L)
            }
        }
    }

    // 초침 정 위치 출발
    fun resetRoundTimer() {
        roundStartTimeMillis = System.currentTimeMillis()
        _intervalProgress.value = 0f
    }


    /** 일시정지 */
    fun pause() {
        if (!isRunning) return

        isRunning = false
        isPaused = true
        pauseTime = _elapsedTime.value

        intervalJob?.cancel()
        countUpJob?.cancel()
        pulseJob?.cancel()
    }

    /** 종료 */
    fun stop() {
        isRunning = false
        isPaused = false
        intervalJob?.cancel()
        countUpJob?.cancel()
        pulseJob?.cancel()
    }

    /** 초기화 */
    fun reset() {
        stop()
        _elapsedTime.value = 0L
        pauseTime = 0L
        roundStartTimeMillis = 0L
        _intervalProgress.value = 0f
    }
}