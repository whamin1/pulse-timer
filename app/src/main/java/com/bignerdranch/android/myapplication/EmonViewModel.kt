package com.bignerdranch.android.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmomViewModel : ViewModel() {

    // 상태 Flow
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _roundRemainingTime = MutableStateFlow(0L)
    val roundRemainingTime: StateFlow<Long> = _roundRemainingTime

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound

    private var _uiState = MutableStateFlow(TimerUIState())
    val uiState: StateFlow<TimerUIState> = _uiState

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    // 내부 상태
    private var totalRounds: Int = 5
    private var roundTime: Long = 60_000L
    private var totalTime: Long = roundTime * totalRounds

    private var isRunning = false
    private var isConfigured = false
    private var isPaused = false
    private var started = false

    private var timerJob: Job? = null
    private var startTime: Long = 0L
    private var pausedAt: Long = 0L

    // 외부 노출 함수
    fun getRoundTime(): Long = roundTime
    fun isRunning(): Boolean = isRunning
    fun isFirstStart(): Boolean = !started
    fun setStatusText(text: String) = {
        _statusText.value = text
    }

    /** 초기 설정 */
    fun configure(rounds: Int, timePerRound: Long) {
        if (isConfigured) return

        totalRounds = rounds
        roundTime = timePerRound
        totalTime = rounds * timePerRound
        isConfigured = true

        _roundRemainingTime.value = roundTime
        _statusText.value = "준비하세요"
        _currentRound.value = 1
        _elapsedTime.value = 0L
        updateUIState(start = true)
    }

    /** 타이머 시작 */
    fun start() {
        started = true

        if (roundTime <= 0 || totalRounds <= 0) return

        isRunning = true

        if (isPaused) {
            startTime += System.currentTimeMillis() - pausedAt
            isPaused = false
        } else{
            startTime = System.currentTimeMillis()
        }

        updateUIState(pause = true, addRound = true)

        timerJob = viewModelScope.launch {
            while (isRunning) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime

                if (elapsed >= totalTime) {
                    stop()
                    updateUIState(reset = true)
                    _statusText.value = "운동 완료"
                    _roundRemainingTime.value = 0L
                    _isFinished.value = true
                    return@launch
                }

                val currentRound = (elapsed / roundTime).toInt() + 1
                val remaining = roundTime - (elapsed % roundTime)


                _elapsedTime.value = elapsed
                _currentRound.value = currentRound
                _roundRemainingTime.value = remaining
                _statusText.value = "운동 중 - 라운드 ${_currentRound.value} / $totalRounds"

                delay(100L)
            }
        }
    }


    /** 정지 */
    fun stop() {
        isRunning = false
        timerJob?.cancel()
    }

    /** 일시정지 */
    fun pause() {
        if (!isRunning || timerJob == null) return

        isRunning = false
        isPaused = true
        pausedAt = System.currentTimeMillis()

        timerJob?.cancel()
        _statusText.value = "일시정지"
        updateUIState(start = true, reset = true)
    }

    /** 초기화 */
    fun reset() {
        isConfigured = false
        started = false
        isRunning = false
        startTime = 0L
        isPaused = false
        pausedAt = 0L

        _roundRemainingTime.value = roundTime
        _statusText.value = "준비하세요"
        _currentRound.value = 1
        timerJob?.cancel()
        _elapsedTime.value = 0L
        _isFinished.value = false
        updateUIState(start = true)
    }

    fun updateUIState(
        start: Boolean = false,
        pause: Boolean = false,
        finish: Boolean = false,
        reset: Boolean = false,
        addRound: Boolean = false
    ) {
        _uiState.value = TimerUIState(
            isStartEnabled = start,
            isPauseEnabled = pause,
            isFinishEnabled = finish,
            isResetEnabled = reset,
            isAddRoundEnabled = addRound
        )
    }
}