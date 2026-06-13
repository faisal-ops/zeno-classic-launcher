package com.zeno.classiclauncher.nlauncher.screensaver

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
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
        @Suppress("DEPRECATION") // DreamService has no setShowWhenLocked(); flag is the only API
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

private class TileState {
    var current: String  = ""
    var previous: String = ""
    var flipProgress: Float = 1f
    var animator: ValueAnimator? = null
}

private class FlipClockDreamView(context: Context) : View(context) {

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    private val topCardPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomCardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(230, 230, 230)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            "sans-serif-condensed", android.graphics.Typeface.BOLD
        )
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 160, 165)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.18f
        typeface = android.graphics.Typeface.create(
            "sans-serif", android.graphics.Typeface.NORMAL
        )
    }

    // Card seam — replaces a plain line
    private val seamGapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(4, 4, 4)
        style = Paint.Style.FILL
    }
    private val seamTopEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // subtle bright catch-light on the bottom edge of the upper card
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val seamBottomShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 0, 0, 0)
        setShadowLayer(24f, 0f, 10f, Color.argb(200, 0, 0, 0))
        style = Paint.Style.FILL
    }
    private val colonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 160, 165)
        style = Paint.Style.FILL
    }
    private val flapShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bounds  = Rect()
    private val camera  = Camera()
    private val matrix  = Matrix()

    private val dateFormatter =
        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    private val hourState   = TileState()
    private val minuteState = TileState()

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            // Sync to wall-clock second boundary to avoid cumulative drift.
            val delay = 1000L - (System.currentTimeMillis() % 1000L)
            postDelayed(this, delay)
        }
    }

    fun start() { removeCallbacks(ticker); post(ticker) }

    fun stop() {
        removeCallbacks(ticker)
        hourState.animator?.cancel()
        minuteState.animator?.cancel()
    }

    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }

    private fun tick() {
        val now     = LocalDateTime.now()
        val use24h  = DateFormat.is24HourFormat(context)
        val hourRaw = now.hour
        val hour    = if (use24h) hourRaw else { val h = hourRaw % 12; if (h == 0) 12 else h }
        updateState(hourState,   hour.toString().padStart(2, '0'))
        updateState(minuteState, now.minute.toString().padStart(2, '0'))
        invalidate()
    }

    private fun updateState(state: TileState, newValue: String) {
        if (state.current == newValue) return
        if (state.current.isEmpty()) { state.current = newValue; state.flipProgress = 1f; return }
        state.previous = state.current
        state.current  = newValue
        state.animator?.cancel()
        state.flipProgress = 0f
        state.animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            addUpdateListener { state.flipProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val now    = LocalDateTime.now()
        val use24h = DateFormat.is24HourFormat(context)
        val period = if (use24h) "" else if (now.hour < 12) "AM" else "PM"

        val w = width.toFloat()
        val h = height.toFloat()
        val landscape = w > h

        val drift = burnInDrift(now)
        canvas.save()
        canvas.translate(drift.first, drift.second)

        val tileGap    = if (landscape) w * 0.040f else w * 0.050f
        val totalWidth = if (landscape) w * 0.62f  else w * 0.72f
        val tileWidth  = (totalWidth - tileGap) / 2f
        val tileHeight = min(if (landscape) h * 0.46f else h * 0.25f, tileWidth * 0.78f)
        val startX     = (w - totalWidth) / 2f
        val startY     = if (landscape) h * 0.27f else h * 0.30f

        drawFlipTile(canvas, RectF(startX, startY, startX + tileWidth, startY + tileHeight), hourState, period)
        drawFlipTile(canvas, RectF(startX + tileWidth + tileGap, startY, startX + totalWidth, startY + tileHeight), minuteState, "")

        // ── Colon separator ─────────────────────────────────────────────────
        val colonX   = startX + tileWidth + tileGap / 2f
        val colonCY  = startY + tileHeight / 2f
        val dotR     = tileHeight * 0.07f
        val dotOff   = tileHeight * 0.18f
        canvas.drawCircle(colonX, colonCY - dotOff, dotR, colonPaint)
        canvas.drawCircle(colonX, colonCY + dotOff, dotR, colonPaint)

        smallPaint.textSize = min(tileHeight * 0.13f, 28f)
        val dateText = now.format(dateFormatter).uppercase(Locale.getDefault()).replace(",", "")
        canvas.drawText(dateText, w / 2f, startY + tileHeight + tileHeight * 0.28f, smallPaint)

        canvas.restore()
    }

    // -----------------------------------------------------------------------
    // Classic two-stage flip animation
    //
    //  Stage 1  (progress 0.0 → 0.5)  quartic ease-in
    //    Old digit TOP HALF rotates backward: top goes away from viewer (0° → 90°)
    //    Pivot at centre line. New digit top half is already visible underneath.
    //
    //  Stage 2  (progress 0.5 → 1.0)  quartic ease-out
    //    New digit BOTTOM HALF rotates forward into view (90° → 0°)
    //    Pivot at centre line. Falls from behind into resting flat position.
    //
    //  Camera is placed closer than default (z = -6) for more dramatic depth.
    // -----------------------------------------------------------------------
    private fun drawFlipTile(canvas: Canvas, rect: RectF, state: TileState, cornerLabel: String) {
        val progress = state.flipProgress
        val cur  = state.current.ifEmpty  { "00" }
        val prev = state.previous.ifEmpty { cur  }

        val cy = rect.centerY()
        val cx = rect.centerX()
        val r  = rect.height() * 0.09f
        val seamH = maxOf(3f, rect.height() * 0.015f)   // physical gap height

        // ── Outer shadow ──────────────────────────────────────────────────
        canvas.drawRoundRect(rect, r, r, shadowPaint)

        // ── Upper card ────────────────────────────────────────────────────
        topCardPaint.shader = LinearGradient(
            0f, rect.top, 0f, cy,
            Color.rgb(46, 46, 46), Color.rgb(30, 30, 30), Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.clipRect(rect.left, rect.top, rect.right, cy - seamH / 2f)
        canvas.drawRoundRect(rect, r, r, topCardPaint)
        canvas.restore()

        // ── Lower card ────────────────────────────────────────────────────
        bottomCardPaint.shader = LinearGradient(
            0f, cy, 0f, rect.bottom,
            Color.rgb(22, 22, 22), Color.rgb(10, 10, 10), Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.clipRect(rect.left, cy + seamH / 2f, rect.right, rect.bottom)
        canvas.drawRoundRect(rect, r, r, bottomCardPaint)
        canvas.restore()

        // ── Static text ───────────────────────────────────────────────────
        canvas.save()
        canvas.clipRect(rect.left, rect.top, rect.right, cy - seamH / 2f)
        drawTileText(canvas, rect, cur, cornerLabel)
        canvas.restore()

        canvas.save()
        canvas.clipRect(rect.left, cy + seamH / 2f, rect.right, rect.bottom)
        drawTileText(canvas, rect, cur, "")
        canvas.restore()

        // ── Animated flap ─────────────────────────────────────────────────
        if (progress < 1f) {

            if (progress <= 0.5f) {
                // Stage 1 — old top half rotates backward (top away from viewer)
                val t      = progress * 2f                          // 0 → 1
                val eased  = t * t * t * t                          // quartic ease-in
                val angle  = eased * 90f                            // 0° → 90°

                buildMatrix(cx, cy, -angle)   // negative = top away from viewer

                canvas.save()
                canvas.clipRect(rect.left, rect.top, rect.right, cy - seamH / 2f)
                canvas.concat(matrix)

                topCardPaint.shader = LinearGradient(
                    0f, rect.top, 0f, cy,
                    Color.rgb(46, 46, 46), Color.rgb(30, 30, 30), Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, r, r, topCardPaint)
                // Darken flap as it rotates (less light hits the surface)
                flapShadowPaint.color = Color.argb((eased * 120).toInt(), 0, 0, 0)
                canvas.drawRoundRect(rect, r, r, flapShadowPaint)
                drawTileText(canvas, rect, prev, cornerLabel)
                canvas.restore()

            } else {
                // Stage 2 — new bottom half rotates forward into view
                val p      = (progress - 0.5f) * 2f                 // 0 → 1
                val eased  = 1f - (1f - p) * (1f - p) * (1f - p) * (1f - p)  // quartic ease-out
                val angle  = (1f - eased) * 90f                     // 90° → 0°

                buildMatrix(cx, cy, angle)    // positive = bottom swings toward viewer

                canvas.save()
                canvas.clipRect(rect.left, cy + seamH / 2f, rect.right, rect.bottom)
                canvas.concat(matrix)

                bottomCardPaint.shader = LinearGradient(
                    0f, cy, 0f, rect.bottom,
                    Color.rgb(22, 22, 22), Color.rgb(10, 10, 10), Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, r, r, bottomCardPaint)
                // Brighten as flap lands flat (light catches it)
                flapShadowPaint.color = Color.argb(((1f - eased) * 100).toInt(), 0, 0, 0)
                canvas.drawRoundRect(rect, r, r, flapShadowPaint)
                drawTileText(canvas, rect, cur, "")
                canvas.restore()
            }
        }

        // ── Card seam — drawn last so it sits on top of everything ────────
        drawCardSeam(canvas, rect, cy, seamH, r)
    }

    // Draws the physical gap between the two half-cards
    private fun drawCardSeam(canvas: Canvas, rect: RectF, cy: Float, seamH: Float, r: Float) {
        val top    = cy - seamH / 2f
        val bottom = cy + seamH / 2f

        // Dark gap
        seamGapPaint.color = Color.rgb(4, 4, 4)
        canvas.drawRect(rect.left, top, rect.right, bottom, seamGapPaint)

        // Catch-light: 1 px bright line at the very bottom of the upper card
        canvas.drawLine(rect.left, top, rect.right, top, seamTopEdgePaint)

        // Shadow gradient below the gap onto the lower card (depth illusion)
        seamBottomShadowPaint.shader = LinearGradient(
            0f, bottom, 0f, bottom + seamH * 3f,
            Color.argb(80, 0, 0, 0), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(rect.left, bottom, rect.right, bottom + seamH * 3f, seamBottomShadowPaint)
    }

    // Builds a 3D perspective matrix pivoted at (pivotX, pivotY).
    // Uses a closer camera (z = -6 instead of default -8) for stronger depth.
    private fun buildMatrix(pivotX: Float, pivotY: Float, angle: Float) {
        camera.save()
        camera.setLocation(0f, 0f, -6f)
        camera.rotateX(angle)
        camera.getMatrix(matrix)
        camera.restore()
        matrix.preTranslate(-pivotX, -pivotY)
        matrix.postTranslate(pivotX, pivotY)
    }

    private fun drawTileText(canvas: Canvas, rect: RectF, text: String, cornerLabel: String) {
        textPaint.textSize = rect.height() * 0.72f
        textPaint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, rect.centerX(), rect.centerY() - bounds.exactCenterY(), textPaint)

        if (cornerLabel.isNotEmpty()) {
            smallPaint.textAlign = Paint.Align.LEFT
            smallPaint.textSize  = rect.height() * 0.14f
            canvas.drawText(
                cornerLabel,
                rect.left   + rect.width()  * 0.07f,
                rect.bottom - rect.height() * 0.14f,
                smallPaint
            )
            smallPaint.textAlign = Paint.Align.CENTER
        }
    }

    private fun burnInDrift(now: LocalDateTime): Pair<Float, Float> {
        val step   = now.minute % 8
        val radius = min(width, height).coerceAtLeast(1) * 0.012f
        val offsets = arrayOf(
            -radius to -radius,  0f to -radius,  radius to -radius,  radius to 0f,
             radius to  radius,  0f to  radius, -radius to  radius, -radius to 0f,
        )
        return offsets[step]
    }
}
