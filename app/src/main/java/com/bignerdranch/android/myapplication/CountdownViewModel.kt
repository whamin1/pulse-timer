package com.bignerdranch.android.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CountdownViewModel : ViewModel() {

    private val _count = MutableStateFlow(-1)
    val count: StateFlow<Int> = _count

    private val _isCounting = MutableStateFlow(false)
    val isCounting: StateFlow<Boolean> = _isCounting

    private var started = false
    private var countdownJob: Job? = null

    /** 카운트다운 시작 (기본 3초) */ //fun startCountdown(seconds: Int = 3, onFinish: () -> Unit)
    fun startCountdown(onFinish: () -> Unit) {
        if (_isCounting.value || started) return

        countdownJob?.cancel()
        started = true
        _isCounting.value = true

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _count.value = i
                delay(1000L)
            }
            _count.value = 0
            delay(500L)
            _isCounting.value = false
            onFinish()
        }
    }

    /** 카운트다운 취소 */
    fun cancel() {
        countdownJob?.cancel()
        _isCounting.value = false
        _count.value = -1
        started = false
    }

    /** 초기화 */
    fun reset() {
       cancel()
    }
}