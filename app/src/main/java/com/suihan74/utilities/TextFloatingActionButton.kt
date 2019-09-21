package com.suihan74.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.R

class TextFloatingActionButton(context: Context, private val attrs : AttributeSet)
    : FloatingActionButton(context, attrs) {

    var text : String = ""
        set(value) {
            field = value

            val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.TextFloatingActionButton,
                0, 0
            )
            try {
                val textColor = typedArray.getColor(R.styleable.TextFloatingActionButton_textColor, Color.WHITE)
                this.setImageBitmap(textToBitmap(value, 40f, textColor))
            }
            finally {
                typedArray.recycle()
            }
        }


    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.TextFloatingActionButton,
            0, 0
        )

        try {
            val text = typedArray.getString(R.styleable.TextFloatingActionButton_text) ?: ""
            val textColor = typedArray.getColor(R.styleable.TextFloatingActionButton_textColor, Color.WHITE)
            this.setImageBitmap(textToBitmap(text, 40f, textColor))
        }
        finally {
            typedArray.recycle()
        }
    }

    fun textToBitmap(text: String, textSize: Float, textColor: Int) : Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setTextSize(textSize)
            color = textColor
            textAlign = Paint.Align.LEFT
        }
        val baseline = -paint.ascent()
        val width = paint.measureText(text).toInt()
        val height = (baseline + paint.descent()).toInt()

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, .0f, baseline, paint)

        return image
    }
}
