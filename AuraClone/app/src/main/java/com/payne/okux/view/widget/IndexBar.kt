package com.payne.okux.view.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * A simple IndexBar widget for displaying alphabetical index indicators.
 * This is a placeholder implementation to satisfy layout requirements.
 */
class IndexBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val indexLetters = arrayOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    )

    private var indexTextColor = Color.BLUE
    private var selectTextColor = Color.RED
    private var indexTextSize = 13f * resources.displayMetrics.scaledDensity
    private var selectedIndex = -1

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = indexTextSize
    }

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, com.auraclone.R.styleable.IndexBar)
            try {
                indexTextColor = typedArray.getColor(
                    com.auraclone.R.styleable.IndexBar_indexTextColor,
                    Color.BLUE
                )
                selectTextColor = typedArray.getColor(
                    com.auraclone.R.styleable.IndexBar_selectTextColor,
                    Color.RED
                )
                indexTextSize = typedArray.getDimension(
                    com.auraclone.R.styleable.IndexBar_indexTextSize,
                    indexTextSize
                )
                textPaint.textSize = indexTextSize
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width == 0 || height == 0) return

        val letterHeight = height.toFloat() / indexLetters.size
        val x = width / 2f

        indexLetters.forEachIndexed { index, letter ->
            textPaint.color = if (index == selectedIndex) selectTextColor else indexTextColor
            val y = (index + 1) * letterHeight - letterHeight / 2 + textPaint.textSize / 3
            canvas.drawText(letter, x, y, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val y = event.y
                val letterHeight = height.toFloat() / indexLetters.size
                val index = (y / letterHeight).toInt().coerceIn(0, indexLetters.size - 1)
                
                if (index != selectedIndex) {
                    selectedIndex = index
                    invalidate()
                    // You can add a callback here if needed
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                selectedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

