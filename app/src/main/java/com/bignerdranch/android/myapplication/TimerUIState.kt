package com.bignerdranch.android.myapplication

data class TimerUIState (
    val isStartEnabled: Boolean = true,
    val isPauseEnabled: Boolean = false,
    val isFinishEnabled: Boolean = false,
    val isResetEnabled: Boolean = false,
    val isAddRoundEnabled: Boolean = false
)