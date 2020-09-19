package com.suihan74.utilities.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.ActionMenuView
import androidx.databinding.BindingAdapter
import com.google.android.material.bottomappbar.BottomAppBar

/** 「右FAB」and「右詰めメニュー」を表示するBottomAppBar */
class CustomBottomAppBar : BottomAppBar {
    var menuGravity: Int = Gravity.START
        internal set

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleInt: Int) : super(
        context,
        attrs,
        defStyleInt
    )

    override fun getActionMenuViewTranslationX(
        actionMenuView: ActionMenuView,
        fabAlignmentMode: Int,
        fabAttached: Boolean
    ): Int {
        return if (menuGravity == Gravity.END && fabAttached && fabAlignmentMode == FAB_ALIGNMENT_MODE_END) {
            // FABがアタッチされている場合通常は左寄せされる
            // それを強制的に右寄せする
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
        else if (menuGravity == Gravity.START && !fabAttached) {
            // FABがアタッチされていない場合通常右寄せされる
            // それを強制的に左寄せにする
            super.getActionMenuViewTranslationX(
                actionMenuView,
                FAB_ALIGNMENT_MODE_END,
                true
            )
        }
        else super.getActionMenuViewTranslationX(
            actionMenuView,
            fabAlignmentMode,
            fabAttached
        )
    }
}

@BindingAdapter("menuGravity")
fun CustomBottomAppBar.bindMenuItemsGravity(gravity: Int?) {
    if (gravity != null) {
        menuGravity = gravity
        refreshDrawableState()
    }
}
