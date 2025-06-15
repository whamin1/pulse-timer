package com.bignerdranch.android.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class TimerActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnFinish: Button
    private lateinit var btnReset: Button
    private lateinit var vibrator: Vibrator
    private lateinit var btnAddRound: FloatingActionButton
    private lateinit var addRoundtText: TextView
    private lateinit var roundCountText: TextView
    private lateinit var roundLogText: TextView

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var currentSet = 1
    private var totalSets = 8
    private var workTime = 20_000L
    private var restTime = 10_000L
    private var timeLeftInMillis = 0L
    private var roundCount = 0
    private var forTimeCapMillis: Long = -1L
    private var resultShown = false
    private var isCountingDown = false
    private var countdownJob: Job? = null
    private var countdownFinishedCallback: (() -> Unit)? = null
    private var lastBackPressedTime: Long = 0L
    private var timerMode: TimerMode = TimerMode.TABATA
    private var timerState = TimerState.STOPPED
    private var previousRound = 1

    private val viewModel: TimerViewModel by viewModels()
    private val tabataViewModel: TabataViewModel by viewModels()
    private val emomViewModel: EmomViewModel by  viewModels()
    private val amrapViewModel: AmrapViewModel by viewModels()
    private val roundViewModel: RoundViewModel by viewModels()
    private val pulseViewModel: PulseViewModel by viewModels()
    private val countdownViewModel: CountdownViewModel by viewModels()


    enum class TimerState{ WORKING, RESTING, STOPPED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 진동기 초기화 (API 31 이상은 VibratorManager 사용)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        timerText = findViewById(R.id.timer_text)
        statusText = findViewById(R.id.status_text)
        btnStart = findViewById(R.id.btn_start)
        btnPause = findViewById(R.id.btn_pause)
        btnFinish = findViewById(R.id.btn_finish)
        btnReset = findViewById(R.id.btn_reset)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        btnAddRound = findViewById(R.id.btn_add_round)
        addRoundtText = findViewById(R.id.btn_add_text)
        roundCountText = findViewById(R.id.text_round_count)
        roundLogText = findViewById(R.id.text_round_log)
        val clockView = findViewById<ClockTimerView>(R.id.clockTimerView)

        val totalDuration = 600_00L
        val intervalDuration = 20_000L
        val startTime = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime

                val totalProgress = (elapsed % totalDuration).toFloat() / totalDuration
                val intervalProgress = (elapsed % intervalDuration).toFloat() / intervalDuration

                clockView.totalProgress = totalProgress
                clockView.intervalProgress = intervalProgress
                clockView.invalidate()

            }
        }
        handler.post(runnable)

        when (timerMode) {
            TimerMode.TABATA -> {
                clockView.showTotalHand = true
                clockView.showIntervalHand = false
            }
            TimerMode.EMOM -> {
                clockView.showTotalHand = true
                clockView.showIntervalHand = false
            }
            TimerMode.AMRAP -> {
                clockView.showTotalHand = true
                clockView.showIntervalHand = false
                clockView.intervalProgress = 0f
            }
            TimerMode.FOR_TIME -> {
                clockView.showTotalHand = true
                clockView.showIntervalHand = true
            }
            TimerMode.PULSE -> {
                clockView.showTotalHand = true
                clockView.showIntervalHand = false
            }
        }

        val timerText = findViewById<TextView>(R.id.timer_text)

        timerText.setOnClickListener {
            if (isTimerRunning()) {
                Toast.makeText(this, "타이머 실행 중엔 열 수 없어요.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

        }



        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = System.currentTimeMillis()
                if (current - lastBackPressedTime < 2000L) {
                    finish()
                } else {
                    lastBackPressedTime = current
                    Toast.makeText(this@TimerActivity, "한 번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                countdownViewModel.count.collect { count ->
                    when (count) {
                        in 1 ..3 -> {
                            timerText.text = count.toString()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(100)
                            }
                    }
                        0 -> {
                            timerText.text = "시작"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(100)
                            }
                        }
                        else -> {
                        timerText.text = ""
                        }
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { 
                countdownViewModel.isCounting.collect { isCounting ->
                    if (!isCounting && countdownFinishedCallback != null) {
                        countdownFinishedCallback?.invoke()
                        countdownFinishedCallback = null
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { 
                roundViewModel.roundCount.collect { count ->
                    roundCountText.text = "$count 라운드"
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { 
                roundViewModel.rounds.collectLatest { entries ->
                    roundLogText.text = entries.joinToString("\n") { entry ->
                        val abs = if (timerMode == TimerMode.FOR_TIME || timerMode == TimerMode.PULSE) {
                            roundViewModel.formatMillisWithSign(entry.absoluteTime)
                        }
                        else roundViewModel.formatMillis(entry.absoluteTime)

                        val interval = if (timerMode == TimerMode.FOR_TIME || timerMode == TimerMode.PULSE) {
                            roundViewModel.formatMillisWithSign(entry.intervalTime)
                        } else {
                            roundViewModel.formatMillis(entry.intervalTime)
                        }
                        "라운드 ${entry.roundNumber}  $abs ($interval)"

                    }
                }
            }
        }
        val modeString = intent.getStringExtra("mode")
        timerMode = TimerMode.valueOf(modeString ?: "TABATA")

        workTime = intent.getLongExtra("workTime", 20_000L)
        restTime = intent.getLongExtra("restTime", 10_000L)
        val prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE)
        totalSets = intent.getIntExtra("sets", -1)

        if (totalSets == -1) {
            totalSets = prefs.getInt("${timerMode.name}_sets", 8)
        }
        forTimeCapMillis = intent.getLongExtra("forTimeCap", -1L)


        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                btnStart.visibility = if (state.isStartEnabled) View.VISIBLE else View.GONE
                btnPause.visibility = if (state.isPauseEnabled) View.VISIBLE else View.GONE
                btnFinish.visibility = if (state.isFinishEnabled) View.VISIBLE else View.GONE
                btnReset.visibility = if (state.isResetEnabled) View.VISIBLE else View.GONE
                btnAddRound.visibility = if (state.isAddRoundEnabled) View.VISIBLE else View.GONE
                addRoundtText.visibility = if (state.isAddRoundEnabled) View.VISIBLE else View.GONE
            }

        }

        if (timerMode == TimerMode.TABATA) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    tabataViewModel.statusText.collectLatest { status ->
                        statusText.text = status
                    }
                }
            }

            tabataViewModel.configure(workTime, restTime, totalSets)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    tabataViewModel.currentSet.collect { round ->
                        roundCountText.text = "세트: $round"
                    }
                }
            }

            tabataViewModel.setStatusText(getString(R.string.status_ready1))

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    tabataViewModel.remainingTime.collect { millis ->
                        timerText.text = formatMillisDeci(millis)

                        val phase = tabataViewModel.phase.value
                        val duration = when (phase) {
                            TabataViewModel.Phase.WORK -> workTime
                            TabataViewModel.Phase.REST -> restTime
                            else -> 1L
                        }

                        val elapsed = (System.currentTimeMillis()) - tabataViewModel.phaseStartTime()
                        val progress = (elapsed.coerceAtLeast(0L) % duration).toFloat() / duration
                        clockView.setProgressSmoothly(progress)
                        clockView.invalidate()
                    }
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    combine(
                        tabataViewModel.phase,
                        tabataViewModel.isPaused
                    ) { phase, isPaused ->
                        phase to isPaused
                    }.collectLatest { (phase, isPaused) ->

                        val currentSet = tabataViewModel.currentSet.value
                        val totalSets = tabataViewModel.getTotalSets()

                        val statusText = if (isPaused) {
                            "일시정지됨"
                        } else {
                            when (phase) {
                                TabataViewModel.Phase.IDLE -> getString(R.string.status_ready1, currentSet, totalSets)
                                TabataViewModel.Phase.WORK -> getString(R.string.status_work, currentSet, totalSets)
                                TabataViewModel.Phase.REST -> getString(R.string.status_work_to_rest, currentSet, totalSets)
                                TabataViewModel.Phase.DONE -> getString(R.string.status_finished)
                            }
                        }

                        tabataViewModel.setStatusText(statusText)

                        clockView.isWorking = (phase == TabataViewModel.Phase.WORK && !isPaused)
                        clockView.animateColorChange(
                            if (phase == TabataViewModel.Phase.WORK && !isPaused)
                                Color.parseColor("#4682B4") else Color.parseColor("#2E8B57")
                        )
                        clockView.invalidate()
                    }
                }
            }


            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    tabataViewModel.phase.collectLatest { phase ->
                        val currentSet = tabataViewModel.currentSet.value
                        val totalSets = tabataViewModel.getTotalSets()

                        val statusText = when (phase) {
                            TabataViewModel.Phase.IDLE -> {
                                clockView.setProgressSmoothly(0f)
                                getString(R.string.status_ready1, currentSet, totalSets)
                            }
                            TabataViewModel.Phase.WORK -> {
                                clockView.setProgressSmoothly(0f)
                                getString(R.string.status_work, currentSet, totalSets)
                            }
                            TabataViewModel.Phase.REST -> {
                                clockView.setProgressSmoothly(0f)
                                getString(R.string.status_work_to_rest, currentSet, totalSets)
                            }
                            TabataViewModel.Phase.DONE -> {
                                viewModel.setButtonState(
                                    start = false,
                                    pause = false,
                                    finish = false,
                                    reset = true,
                                    addRound = false)
                                showResultDialog()
                                getString(R.string.status_finished)
                            }
                        }
                        tabataViewModel.setStatusText(statusText)

                        clockView.isWorking = (phase == TabataViewModel.Phase.WORK)
                        val targetColor = if (phase == TabataViewModel.Phase.WORK) Color.parseColor("#4682B4") else {
                            Color.parseColor("#2E8B57")
                        }
                        clockView.animateColorChange(targetColor)
                        clockView.invalidate()
                    }
                }
            }
        }


        if (timerMode == TimerMode.EMOM) {
            emomViewModel.configure(totalSets, workTime)

            emomViewModel.setStatusText(getString(R.string.status_ready1, currentSet, totalSets))

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    emomViewModel.currentRound.collectLatest { round ->
                        roundCountText.text = "세트: $round"
                    }
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    emomViewModel.uiState.collect { state ->
                        btnStart.isEnabled = state.isStartEnabled
                        btnPause.isEnabled = state.isPauseEnabled
                        btnFinish.isEnabled = state.isFinishEnabled
                        btnReset.isEnabled = state.isResetEnabled
                        btnAddRound.isEnabled = state.isAddRoundEnabled
                    }
                }
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    emomViewModel.roundRemainingTime.collect { millis ->
                        timerText.text = formatMillisDeci(millis)
                        val elapsed = emomViewModel.elapsedTime.value
                        val roundTime = emomViewModel.getRoundTime()
                        val currentRound = (elapsed / roundTime).toInt() + 1
                        90
                        if (currentRound != previousRound) {
                            previousRound = currentRound
                            clockView.setProgressSmoothly(0f)
                        }

                        if (roundTime != 0L) {
                            val progress = (elapsed % roundTime).toFloat() / roundTime
                            clockView.setProgressSmoothly(progress)
                        } else {
                            clockView.setProgressSmoothly(0f)
                        }
                    }
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    emomViewModel.statusText.collect {
                        statusText.text = it
                    }
                }
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) { 
                    emomViewModel.uiState.collect { state ->
                        btnStart.isEnabled = state.isStartEnabled
                        btnPause.isEnabled = state.isPauseEnabled
                        btnFinish.isEnabled = state.isFinishEnabled
                        btnReset.isEnabled = state.isResetEnabled
                        btnAddRound.isEnabled = state.isAddRoundEnabled
                    }
                }
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    emomViewModel.isFinished.collect { finished ->
                        if (finished) {
                            showResultDialog()
                        }
                    }
                }
            }

        }

        if (timerMode == TimerMode.AMRAP) {
            amrapViewModel.configure(workTime)
            statusText.text = "${formatMillis(workTime)}"

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    amrapViewModel.remainingTime.collectLatest { elapsed ->
                        timerText.text = formatMillisDeci(elapsed)
                    }
                }
            }

            lifecycleScope.launch {
                amrapViewModel.totalProgress.collectLatest { progress ->
                    clockView.setProgressSmoothly(progress)
                }
            }
            lifecycleScope.launch {
                amrapViewModel.intervalProgress.collectLatest { progress ->
                    if (amrapViewModel.getRoundStartTime() != 0L) {
                        clockView.setIntervalProgressSmoothly(progress)
                    } else {
                        clockView.setIntervalProgressSmoothly(0f)
                    }

                }
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    amrapViewModel.isFinished.collectLatest { finished ->
                        if (finished && !resultShown) {
                            resultShown = true
                            viewModel.setButtonState(
                                start = false,
                                pause = false,
                                finish = false,
                                reset = true,
                                addRound = false
                            )
                            amrapViewModel.setStatusText(getString(R.string.status_finished))
                            showResultDialog()
                        }
                    }
                }
            }
        }


        if (timerMode == TimerMode.FOR_TIME) {
            viewModel.timeCap = forTimeCapMillis
            clockView.showIntervalHand = true
            clockView.showTotalHand = (forTimeCapMillis > 0)
            statusText.text = "${formatMillis(forTimeCapMillis)}"

            if (forTimeCapMillis == 0L) {
                lifecycleScope.launch {
                    viewModel.intervalProgress.collectLatest { progress ->
                        clockView.setIntervalProgressSmoothly(progress)
                    }
                }
            }

            lifecycleScope.launch {
                viewModel.intervalProgress.collectLatest { progress ->
                    clockView.setIntervalProgressSmoothly(progress)
                }
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.elapsedTime.collectLatest { elapsed ->
                        timerText.text = formatMillisPrecis(elapsed)

                        if (forTimeCapMillis > 0) {
                            val totalProgress = elapsed.toFloat() / forTimeCapMillis
                            clockView.setProgressSmoothly(totalProgress)
                        } else {
                            val totalProgress = (elapsed % 60_000L).toFloat() / 60_000L
                            clockView.setProgressSmoothly(totalProgress)
                        }

                        if (forTimeCapMillis > 0 && elapsed >= forTimeCapMillis) {
                            statusText.text = "운동 완료"
                            viewModel.setButtonState(
                                start = false,
                                pause = false,
                                finish = false,
                                reset = true,
                                addRound = false
                            )


                            if (!resultShown) {
                                resultShown = true
                                showResultDialog()
                            }
                        }
                    }
                }
            }
        }


        btnAddRound.setOnClickListener {
            val elapsedTime = when (timerMode) {
                TimerMode.FOR_TIME -> {
                    viewModel.resetRoundTimer()
                    viewModel.startIntervalTracking()
                    viewModel.elapsedTime.value
                }
                TimerMode.AMRAP -> {
                    amrapViewModel.startRoundTimer()
                    workTime - amrapViewModel.remainingTime.value
                }
                TimerMode.PULSE -> pulseViewModel.elapsedTime.value
                else -> 0L
            }

            roundCount++
            roundCountText.text = "라운드: $roundCount"

            if (timerMode == TimerMode.AMRAP) {
                clockView.showIntervalHand = true
                clockView.invalidate()
            }

            roundViewModel.addRound(
                currentRemainingTime = elapsedTime,
                timerMode = timerMode,
                customTotalTime = if (timerMode == TimerMode.AMRAP) workTime else 0L
            )

            if (timerMode == TimerMode.PULSE) {
                pulseViewModel.startIntervalTracking()
                pulseViewModel.resetRoundTimer()
                pulseViewModel.startNewRound(onPulse = { vibrate(400) }, fromPausedState = false)
                viewModel.setButtonState(
                    start = false,
                    pause = true,
                    finish = true,
                    reset = false,
                    addRound = true
                )
            }
        }


        if (timerMode == TimerMode.PULSE) {

            lifecycleScope.launch {
                pulseViewModel.intervalProgress.collectLatest { progress ->
                    clockView.setProgressSmoothly(progress)
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    pulseViewModel.elapsedTime.collectLatest { elapsed ->
                        timerText.text = formatMillisPrecis(elapsed)
                    }
                }
            }
        }

        btnStart.setOnClickListener {
            viewModel.setButtonState(
                start = false,
                pause = true,
                finish = false,
                reset = false,
                addRound = false
            )
            when (timerMode) {
                TimerMode.FOR_TIME -> {
                    val elapsed = viewModel.elapsedTime.value
                    if (elapsed == 0L) {
                        startWithCount {
                            viewModel.start()
                            roundViewModel.setStartTime(viewModel.elapsedTime.value)
                            viewModel.setButtonState(
                                start = false,
                                pause = true,
                                finish = true,
                                reset = false,
                                addRound = true
                            )
                        }
                    } else {
                        viewModel.start()
                        viewModel.setButtonState(
                            start = false,
                            pause = true,
                            finish = true,
                            reset = false,
                            addRound = true
                        )
                    }
                }


                TimerMode.AMRAP -> {
                    if (amrapViewModel.isFirstStart()) {
                        startWithCount  {
                            amrapViewModel.start()
                            handler.postDelayed(runnable, 30L)
                            roundViewModel.setStartTime(amrapViewModel.remainingTime.value)
                            roundViewModel.setTotalTime(workTime)
                            viewModel.setButtonState(
                                start = false,
                                pause = true,
                                finish = true,
                                reset = false,
                                addRound = true
                            )

                        }
                    } else {
                        amrapViewModel.resume()
                        handler.postDelayed(runnable, 30L)
                        viewModel.setButtonState(
                            start = false,
                            pause = true,
                            finish = true,
                            reset = false,
                            addRound = true
                        )
                    }
                }

                TimerMode.PULSE -> {
                    val elapsed = pulseViewModel.elapsedTime.value

                    if (elapsed == 0L) {
                        startWithCount  {
                            pulseViewModel.resetRoundTimer()
                            pulseViewModel.startIntervalTracking()
                            viewModel.setButtonState(
                                start = false,
                                pause = true,
                                finish = true,
                                reset = false,
                                addRound = true
                            )
                            pulseViewModel.startNewRound(onPulse = { vibrate(400)}, fromPausedState = true)
                        }
                    } else {
                        pulseViewModel.startNewRound(
                            onPulse = {vibrate(400)},
                            fromPausedState = true
                        )
                        viewModel.setButtonState(
                            start = false,
                            pause = true,
                            finish = true,
                            reset = false,
                            addRound = true
                        )
                        pulseViewModel.startNewRound(onPulse = { vibrate(400)}, fromPausedState = true)
                    }

                }

                TimerMode.TABATA -> {
                    if (tabataViewModel.isFirstStart()) {
                        startWithCount  {
                            tabataViewModel.start()
                            handler.postDelayed(runnable, 30L)
                            val currentSet = tabataViewModel.currentSet.value
                            val totalSets = tabataViewModel.getTotalSets()
                            getString(R.string.status_work, currentSet, totalSets)
                            viewModel.setButtonState(
                                start = false,
                                pause = true,
                                finish = false,
                                reset = false,
                                addRound = false
                            )
                            roundViewModel.setStartTime(System.currentTimeMillis())
                        }
                    } else {
                        tabataViewModel.resume()
                        handler.postDelayed(runnable, 30L)
                    }
                }

                TimerMode.EMOM -> {

                    if (emomViewModel.isFirstStart()) {
                        emomViewModel.updateUIState(pause = true)
                        startWithCount  {
                            viewModel.setButtonState(
                                start = false,
                                pause = true,
                                finish = false,
                                reset = false,
                                addRound = false
                            )
                            emomViewModel.start()
                            handler.postDelayed(runnable, 30L)
                            roundViewModel.setStartTime(System.currentTimeMillis())
                        }
                    } else {
                        viewModel.setButtonState(
                            start = false,
                            pause = true,
                            finish = false,
                            reset = false,
                            addRound = false
                        )
                        emomViewModel.start()
                        handler.postDelayed(runnable, 30L)
                    }
                }
            }
        }

        btnPause.setOnClickListener {

            when (timerMode) {
                TimerMode.TABATA -> {
                    tabataViewModel.pause()
                    viewModel.setButtonState(
                        start = true,
                        pause = false,
                        finish = false,
                        reset = true,
                        addRound = false
                    )
                    handler.removeCallbacks(runnable)
                    updateStatusTextIf(TimerMode.TABATA, R.string.status_paused)
                }
                TimerMode.EMOM -> {viewModel.setButtonState(
                    start = true,
                    pause = false,
                    finish = false,
                    reset = true,
                    addRound = false
                )
                    handler.removeCallbacks(runnable)
                    emomViewModel.pause()
                }
                TimerMode.AMRAP ->{
                    viewModel.setButtonState(
                        start = true,
                        pause = false,
                        finish = true,
                        reset = true,
                        addRound = false
                    )
                    amrapViewModel.pause()
                    handler.removeCallbacks(runnable)
                    resultShown = false

                }
                TimerMode.FOR_TIME -> {
                    viewModel.pause()
                    viewModel.setButtonState(
                        start = true,
                        pause = false,
                        finish = true,
                        reset = true,
                        addRound = false
                    )
                }
                TimerMode.PULSE -> {
                    pulseViewModel.pause()
                    handler.removeCallbacks(runnable)
                    viewModel.setButtonState(
                        start = true,
                        pause = false,
                        finish = true,
                        reset = true,
                        addRound = true
                    )
               }
            }
            if (isCountingDown) {

                countdownJob?.cancel()
                countdownViewModel.cancel()
                isCountingDown = false
                timerText.text = "일시정지"
                viewModel.setButtonState(
                    start = true,
                    pause = false,
                    finish = false,
                    reset = false,
                    addRound = false
                )
                return@setOnClickListener
            }
        }

        btnFinish.setOnClickListener {
            statusText.text = "운동 완료"
            viewModel.setButtonState(
                start = false,
                pause = false,
                finish = false,
                reset = true,
                addRound = false
            )

            if (timerMode == TimerMode.AMRAP) {

                val elapsed = workTime - amrapViewModel.remainingTime.value
                if (roundViewModel.lastTime - elapsed > 1000L) {
                    roundCount += 1
                    roundCountText.text = "라운드: $roundCount"
                    roundViewModel.addRound(elapsed, TimerMode.AMRAP, workTime)
                }
                amrapViewModel.pause()
            }

            when (timerMode) {
                TimerMode.FOR_TIME -> {
                    val elapsed = viewModel.elapsedTime.value
                    roundCount += 1
                    roundCountText.text = "라운드: $roundCount"
                    if (elapsed - roundViewModel.lastTime > 1000L) {
                        roundViewModel.addRound(
                            currentRemainingTime = elapsed,
                            timerMode = TimerMode.FOR_TIME,
                            customTotalTime = 0L
                        )
                    }
                    viewModel.pause()
                }

                TimerMode.PULSE -> {
                    val elapsed = pulseViewModel.elapsedTime.value
                    if (elapsed > 1000L) {
                        roundCount += 1
                        roundCountText.text = "라운드: $roundCount"
                        roundViewModel.addRound(
                            currentRemainingTime = elapsed,
                            timerMode = TimerMode.PULSE,
                            customTotalTime = 0L
                        )
                    }
                    pulseViewModel.stop()
                }
                else -> {}
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (timerMode == TimerMode.AMRAP || timerMode == TimerMode.FOR_TIME || timerMode == TimerMode.PULSE) {
                    showResultDialog()
                }
            }, 100L)

        }

        btnReset.setOnClickListener {
            when (timerMode) {
                TimerMode.TABATA -> {
                    statusText.text = getString(R.string.status_ready1)
                    tabataViewModel.reset()
                    tabataViewModel.configure(workTime, restTime, totalSets)
                }
                TimerMode.EMOM -> {
                    emomViewModel.reset()
                    emomViewModel.configure(totalSets, workTime)
                }
                TimerMode.AMRAP -> {
                    statusText.text = "준비 완료"
                    roundCount = 0
                    roundCountText.text = "라운드: $roundCount"
                    clockView.showIntervalHand = false
                    amrapViewModel.reset()
                    amrapViewModel.configure(workTime)
                }
                TimerMode.FOR_TIME -> {
                    statusText.text = "준비 완료"
                    roundCount = 0
                    roundCountText.text = "라운드: $roundCount"
                    viewModel.reset()
                    viewModel.startIntervalTracking()
                    viewModel.resetRoundTimer()
                }
                TimerMode.PULSE -> {
                    statusText.text = "준비 완료"
                    roundCount = 0
                    roundCountText.text = "라운드: $roundCount"
                    pulseViewModel.reset()
                }
            }

            countdownViewModel.reset()
            roundViewModel.reset()
            viewModel.setButtonState(
                start = true,
                pause = false,
                finish = false,
                reset = false,
                addRound = false
            )
        }
    }

    private fun updateStatusTextIf(mode: TimerMode, resId: Int) {
        val status = getString(resId)

        when (mode) {
            TimerMode.TABATA -> tabataViewModel.setStatusText(status)
            TimerMode.EMOM -> TODO()
            TimerMode.AMRAP -> TODO()
            TimerMode.FOR_TIME -> TODO()
            TimerMode.PULSE -> TODO()
        }
    }


    private fun startWork() {
        timerState = TimerState.WORKING
        statusText.text = if (timerMode == TimerMode.TABATA || timerMode == TimerMode.AMRAP) {
            "운동 중"
        } else {
            "운동 중 - 세트 $currentSet / $totalSets"
        }
        val duration = timeLeftInMillis.takeIf { it > 0L } ?: workTime
        timeLeftInMillis = duration

        timerText.text = formatMillis(duration)

        countDownTimer = object : CountDownTimer(duration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                timerText.text = formatMillis(millisUntilFinished)
            }

            override fun onFinish() {
                vibrate(800)
                timeLeftInMillis = 0L
                timerText.text = "00:00"
                if (currentSet < totalSets) {
                    startRest()
                } else {
                    timerState = TimerState.STOPPED
                    statusText.text = "운동 완료"
                    timerText.text = ""
                }
            }
        }.start()
        isRunning = true
    }

    private fun startWithCount(onFinish: () -> Unit) {
        isCountingDown = true
        countdownViewModel.startCountdown{
            isCountingDown = false
            onFinish()
        }
    }

    private fun startRest() {
        timerState = TimerState.RESTING
        statusText.text = "휴식 중 - 세트 $currentSet / $totalSets"
        val duration = timeLeftInMillis.takeIf { it > 0L } ?: restTime
        timeLeftInMillis = duration

        countDownTimer = object : CountDownTimer(duration + 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                timerText.text = formatMillis(millisUntilFinished)
            }

            override fun onFinish() {
                vibrate(500)
                timeLeftInMillis = 0L
                currentSet++
                startWork()
            }
        }.start()
        isRunning = true
    }

    private fun vibrate(duration: Long = 500L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun formatMillis(millis: Long): String {
        val safeMills = if (millis < 0) 0 else millis
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatMillisPrecis(millis: Long): String {
        val absMillis = kotlin.math.abs(millis)
        val minutes = (absMillis / 1000) / 60
        val seconds = (absMillis / 1000) % 60
        val centiseconds = (absMillis % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }

    private fun formatMillisDeci(millis: Long): String {
        val absMillis = kotlin.math.abs(millis)
        val minutes = (absMillis / 1000) / 60
        val seconds = (absMillis / 1000) % 60
        val centiseconds = (absMillis % 1000) / 100
        return String.format("%02d:%02d.%01d", minutes, seconds, centiseconds)
    }

    private fun saveWorkoutRecordWithRound(
        record: WorkoutRecordEntity,
        roundList: List<RoundEntry>
    ){
        val db = AppDatabase.getDatabase(applicationContext)

        lifecycleScope.launch {
            val recordId = db.workoutRecordDao().insert(record)

            Log.d("SaveDebug", "저장된 recordId = $recordId")

            val roundEntities = roundList.mapIndexed { index, round ->
                WorkoutRoundEntity(
                    recordId = recordId,
                    roundNumber = index + 1,
                    absoluteTime = round.absoluteTime,
                    intervalTime = round.intervalTime
                )
            }
            Log.d("SaveDebug", "저장할 라운드 수 = ${roundEntities.size}")
            roundEntities.forEach {
                Log.d("SaveDebug", it.toString())
            }
            db.workoutRoundDao().insertAll(roundEntities)
        }
    }

    private fun showResultDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_result, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val messageText = dialogView.findViewById<TextView>(R.id.text_result_message)
        val totalTimeText = dialogView.findViewById<TextView>(R.id.text_total_time)
        val totalRoundText = dialogView.findViewById<TextView>(R.id.text_total_round)
        val avgTimeText = dialogView.findViewById<TextView>(R.id.text_avg_time)
        val closeButton = dialogView.findViewById<Button>(R.id.btn_close)
        val btnRestart = dialogView.findViewById<Button>(R.id.btn_restart)
        val btaHome = dialogView.findViewById<Button>(R.id.btn_home)
        val roundList = when (timerMode) {
            TimerMode.TABATA, TimerMode.EMOM -> {
                List(totalSets) { i ->
                    val absolute = (i + 1) * workTime
                    val interval = workTime
                    RoundEntry(i + 1, absoluteTime = absolute, intervalTime = interval)
                }
            }
            else -> roundViewModel.rounds.value
        }

        val totalTime: Long = when (timerMode) {
            TimerMode.FOR_TIME -> viewModel.elapsedTime.value
            TimerMode.AMRAP -> workTime
            TimerMode.PULSE -> roundList.sumOf { it.absoluteTime }
            TimerMode.TABATA, TimerMode.EMOM -> {
                workTime*totalSets
            }
        }



        val formatted = when (timerMode) {
            TimerMode.FOR_TIME -> formatMillisPrecis(totalTime)
            TimerMode.AMRAP -> formatMillis(totalTime)
            TimerMode.PULSE -> formatMillisPrecis(totalTime)
            TimerMode.TABATA, TimerMode.EMOM -> formatMillis(totalTime)
        }

        totalTimeText.text = "총 소요 시간: ${formatted}"

        totalRoundText.text = "라운드 수: ${when (timerMode) {
            TimerMode.FOR_TIME, TimerMode.AMRAP, TimerMode.PULSE -> roundList.size
            else -> totalSets
        }}"

        val avgTime = when (timerMode) {
            TimerMode.PULSE -> {
                if (roundList.isNotEmpty()) {
                    roundList.map { it.absoluteTime }.average().toLong()
                } else 0L
            }
            TimerMode.FOR_TIME, TimerMode.AMRAP -> {
                if (roundList.isNotEmpty()) {
                    roundList.map { it.intervalTime }.average().toLong()
                } else 0L
            }
            else -> {
                if (totalSets > 0) {
                    totalTime / totalSets
                } else 0L
            }
        }

        avgTimeText.text = "평균 라운드 시간: ${
            when (timerMode) {
                TimerMode.FOR_TIME, TimerMode.PULSE, TimerMode.AMRAP -> formatMillisPrecis(avgTime)
                else -> formatMillisDeci(avgTime) 
            }
        }"



        messageText.text = when (timerMode) {
            TimerMode.AMRAP -> "운동을 완료 \n ${roundList.size}"
            TimerMode.FOR_TIME -> "운동을 완료 \n $formatted"
            TimerMode.TABATA -> "운동을 완료\n총 세트: ${roundList.size}"
            TimerMode.EMOM -> "운동을 완료\n총 세트: ${roundList.size}"
            else -> "운동을 완료함"
        }

        val recordEntity = WorkoutRecordEntity(
            mode = timerMode.name,
            totalTime = totalTime,
            roundCount = roundList.size,
            avgRoundTime = avgTime,
            timestamp = System.currentTimeMillis()
        )

        saveWorkoutRecordWithRound(recordEntity, roundList)


        btnRestart.setOnClickListener {
            dialog.dismiss()
            when (timerMode) {
                TimerMode.AMRAP -> {
                    amrapViewModel.reset()
                    amrapViewModel.configure(workTime)
                }
                TimerMode.FOR_TIME -> {
                    roundCount = 0
                    roundCountText.text = getString(R.string.rounds, roundCount)
                    viewModel.reset()
                }
                TimerMode.PULSE -> {
                    roundCount = 0
                    statusText.text = ""
                    roundCountText.text = getString(R.string.rounds, roundCount)
                    pulseViewModel.reset()
                }
                TimerMode.TABATA -> {
                    tabataViewModel.reset()
                    roundViewModel.reset()
                    tabataViewModel.configure(workTime, restTime, totalSets)
                }
                TimerMode.EMOM ->  {
                    emomViewModel.reset()
                    roundViewModel.reset()
                    emomViewModel.configure(totalSets, workTime)
                }
            }
            countdownViewModel.reset()
            roundViewModel.reset()
            viewModel.setButtonState(
                start = true,
                pause = false,
                finish = false,
                reset = false,
                addRound = false
            )
        }

        btaHome.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    fun isTimerRunning(): Boolean {
        return when (timerMode) {
            TimerMode.TABATA -> tabataViewModel.isRunning()
            TimerMode.EMOM -> emomViewModel.isRunning()
            TimerMode.AMRAP -> amrapViewModel.isRunning()
            TimerMode.FOR_TIME -> viewModel.isRunning()
            TimerMode.PULSE -> pulseViewModel.isRunning()
        }
    }
}
