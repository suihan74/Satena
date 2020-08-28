package com.suihan74.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.ActionMenuView
import com.google.android.material.bottomappbar.BottomAppBar
import com.suihan74.satena.R

/** 「右FAB」and「右詰めメニュー」を表示するBottomAppBar */
class CustomBottomAppBar : BottomAppBar {
    private var menuGravity: Int = 0

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleInt: Int) : super(
        context,
        attrs,
        defStyleInt
    ) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomBottomAppBar,
            defStyleInt,
            com.google.android.material.R.style.Widget_MaterialComponents_BottomAppBar
        )

        try {
            menuGravity = a.getInteger(R.styleable.CustomBottomAppBar_menuGravity, Gravity.START)
        }
        finally {
            a.recycle()
        }
    }

    override fun getActionMenuViewTranslationX(
        actionMenuView: ActionMenuView,
        fabAlignmentMode: Int,
        fabAttached: Boolean
    ): Int {
        return if (menuGravity == Gravity.END && fabAttached && fabAlignmentMode == FAB_ALIGNMENT_MODE_END) {
            val translationX = super.getActionMenuViewTranslationX(
                actionMenuView,
                fabAlignmentMode,
                false
            )

            val fabSize = 56  // normal: 56dp, mini: 40dp
            val additionalMargin = 16
            val density = context.resources.displayMetrics.density
            val fabMargin = (fabCradleMargin + fabCradleRoundedCornerRadius * 2 + (fabSize + additionalMargin) * density).toInt()
            translationX - fabMargin
        }
        else super.getActionMenuViewTranslationX(
            actionMenuView,
            fabAlignmentMode,
            fabAttached
        )
    }
}
