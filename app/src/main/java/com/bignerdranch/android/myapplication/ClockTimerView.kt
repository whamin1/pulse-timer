package com.bignerdranch.android.myapplication

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClockTimerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var totalProgress: Float = 0f
    var intervalProgress: Float = 0f
    var showTotalHand: Boolean = true
    var showIntervalHand: Boolean = true
    var isWorking: Boolean = true

    private var currentHandColor: Int = Color.BLUE

    private val handPaint = Paint().apply {
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val totalPaint = Paint().apply {
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val intervalPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 10f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 20

        //전체 타이머 핸드
        if (showTotalHand) {
            totalPaint.color = if (isWorking) Color.parseColor("#3682B4") else Color.GREEN
            drawHand(canvas, cx, cy, radius, totalProgress, totalPaint)
        }

        //인터벌 핸드
        if (showIntervalHand) {
            drawHand(canvas, cx, cy, radius * 0.85f, intervalProgress, intervalPaint)
        }

        //시작 강조용 핸드
        handPaint.color = currentHandColor
        drawHand(canvas, cx, cy, radius, totalProgress, handPaint)

    }

    private fun drawHand(
        canvas: Canvas,
        cx: Float, cy: Float,
        r: Float, process: Float,
        paint: Paint
    ) {
        val angle = 360f*process - 90f
        val endX = (cx + r* cos(Math.toRadians(angle.toDouble()))).toFloat()
        val endY = (cy + r* sin(Math.toRadians(angle.toDouble()))).toFloat()
        canvas.drawLine(cx, cy, endX, endY, paint)
    }

    /** 부드러운 전체 초침 이동*/
    fun setProgressSmoothly(target: Float) {
        val start = this.totalProgress
        val end = if (target < start) target + 1f else target

        ValueAnimator.ofFloat(start, end).apply {
            duration = 40L
            interpolator = LinearInterpolator()
            addUpdateListener {
                totalProgress = (it.animatedValue as Float) % 1f
                invalidate()
            }
            start()
        }
    }

    /** 부드러운 인터벌 초침 이동 */
    fun setIntervalProgressSmoothly(target: Float) {
        val start = this.intervalProgress
        val end = if (target < start) target + 1f else target

        ValueAnimator.ofFloat(start, end).apply {
            duration = 40L
            interpolator = LinearInterpolator()
            addUpdateListener {
                intervalProgress = (it.animatedValue as Float) % 1f
                invalidate()
            }
            start()
        }
    }

    /** 초침 색상 애니메이션 변경 */
    fun animateColorChange(toColor: Int, duration: Long = 500L) {
        val fromColor = currentHandColor // 현재 초침 색상 저장
        ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            this.duration = duration
            addUpdateListener {
                currentHandColor = it.animatedValue as Int
                invalidate()  // View 다시 그리기
            }
            start()
        }
    }
}