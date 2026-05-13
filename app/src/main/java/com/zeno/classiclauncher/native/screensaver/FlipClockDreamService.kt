package com.zeno.classiclauncher.nlauncher.screensaver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.service.dreams.DreamService
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

class FlipClockDreamService : DreamService() {
    private lateinit var clockView: FlipClockDreamView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        setInteractive(false)
        setFullscreen(true)
        setScreenBright(false)
        clockView = FlipClockDreamView(this)
        setContentView(clockView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        clockView.start()
    }

    override fun onDreamingStopped() {
        clockView.stop()
        super.onDreamingStopped()
    }
}

private class FlipClockDreamView(context: Context) : View(context) {
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 226, 226)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(165, 165, 170)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.18f
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(8, 8, 8)
        strokeWidth = 3f
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(125, 0, 0, 0)
        setShadowLayer(20f, 0f, 8f, Color.argb(180, 0, 0, 0))
    }
    private val bounds = Rect()
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, 1000L)
        }
    }

    fun start() {
        removeCallbacks(ticker)
        post(ticker)
    }

    fun stop() {
        removeCallbacks(ticker)
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val now = LocalDateTime.now()
        val use24h = DateFormat.is24HourFormat(context)
        val hourRaw = now.hour
        val hour = if (use24h) hourRaw else {
            val h = hourRaw % 12
            if (h == 0) 12 else h
        }
        val minute = now.minute
        val hourText = hour.toString().padStart(2, '0')
        val minuteText = minute.toString().padStart(2, '0')
        val period = if (use24h) "" else if (hourRaw < 12) "AM" else "PM"

        val drift = burnInDrift(now)
        canvas.save()
        canvas.translate(drift.first, drift.second)

        val w = width.toFloat()
        val h = height.toFloat()
        val landscape = w > h
        val tileGap = if (landscape) w * 0.035f else w * 0.045f
        val totalWidth = if (landscape) w * 0.56f else w * 0.82f
        val tileWidth = (totalWidth - tileGap) / 2f
        val tileHeight = min(if (landscape) h * 0.46f else h * 0.25f, tileWidth * 0.78f)
        val startX = (w - totalWidth) / 2f
        val startY = if (landscape) h * 0.27f else h * 0.30f
        val radius = tileHeight * 0.09f

        drawFlipTile(canvas, RectF(startX, startY, startX + tileWidth, startY + tileHeight), hourText, period)
        drawFlipTile(
            canvas,
            RectF(startX + tileWidth + tileGap, startY, startX + totalWidth, startY + tileHeight),
            minuteText,
            "",
        )

        smallPaint.textSize = min(tileHeight * 0.13f, 28f)
        val dateText = now.format(dateFormatter).uppercase(Locale.getDefault()).replace(",", "")
        canvas.drawText(dateText, w / 2f, startY + tileHeight + tileHeight * 0.28f, smallPaint)

        canvas.restore()
    }

    private fun drawFlipTile(canvas: Canvas, rect: RectF, text: String, cornerLabel: String) {
        shadowPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, rect.height() * 0.09f, rect.height() * 0.09f, shadowPaint)
        tilePaint.shader = LinearGradient(
            0f,
            rect.top,
            0f,
            rect.bottom,
            Color.rgb(31, 31, 31),
            Color.rgb(14, 14, 14),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, rect.height() * 0.09f, rect.height() * 0.09f, tilePaint)
        tilePaint.shader = null

        dividerPaint.strokeWidth = maxOf(2f, rect.height() * 0.012f)
        canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), dividerPaint)

        textPaint.textSize = rect.height() * 0.72f
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val baseline = rect.centerY() - bounds.exactCenterY()
        canvas.drawText(text, rect.centerX(), baseline, textPaint)

        if (cornerLabel.isNotEmpty()) {
            smallPaint.textAlign = Paint.Align.LEFT
            smallPaint.textSize = rect.height() * 0.10f
            canvas.drawText(cornerLabel, rect.left + rect.width() * 0.07f, rect.bottom - rect.height() * 0.14f, smallPaint)
            smallPaint.textAlign = Paint.Align.CENTER
        }
    }

    private fun burnInDrift(now: LocalDateTime): Pair<Float, Float> {
        val step = now.minute % 8
        val radius = min(width, height).coerceAtLeast(1) * 0.012f
        val offsets = arrayOf(
            -radius to -radius,
            0f to -radius,
            radius to -radius,
            radius to 0f,
            radius to radius,
            0f to radius,
            -radius to radius,
            -radius to 0f,
        )
        return offsets[step]
    }
}
