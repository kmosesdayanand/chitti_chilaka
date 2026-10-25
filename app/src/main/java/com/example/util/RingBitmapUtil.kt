package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object RingBitmapUtil {

    /**
     * Draws an anti-aliased circular progress ring on a transparent bitmap.
     * Starts from -90 degrees (12 o'clock position).
     */
    fun createProgressRingBitmap(
        progress: Float, // 0.0f to 1.0f
        colorHex: String,
        sizePx: Int = 220,
        strokeWidthPx: Float = 20f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val mainColor = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#38BDF8")
        }

        val trackColor = Color.argb(
            45,
            Color.red(mainColor),
            Color.green(mainColor),
            Color.blue(mainColor)
        )

        val padding = strokeWidthPx / 2f + 4f
        val oval = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // Background track circle
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = trackColor
        }
        canvas.drawOval(oval, trackPaint)

        // Progress arc
        val sweepAngle = (progress.coerceIn(0.01f, 1f) * 360f)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            color = mainColor
        }
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        return bitmap
    }
}
