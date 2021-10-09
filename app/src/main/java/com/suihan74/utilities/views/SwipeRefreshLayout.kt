package com.suihan74.utilities.views

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.getThemeColor

/**
 * プログレスバーの色情報をレイアウト側でセットできる`SwipeRefreshLayout`（前景色が一色限定の場合）
 */
class SwipeRefreshLayout(
    context : Context,
    attrs : AttributeSet?,
) : SwipeRefreshLayout(context, attrs) {
    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SwipeRefreshLayout, 0, 0)
        runCatching {
            a.getColor(
                R.styleable.SwipeRefreshLayout_progressBackgroundColor,
                context.getThemeColor(R.attr.swipeRefreshBackground)
            )
        }.onSuccess { backgroundColor ->
            setProgressBackgroundColorSchemeColor(backgroundColor)
        }

        runCatching {
            a.getColor(
                R.styleable.SwipeRefreshLayout_progressForegroundColor,
                context.getThemeColor(R.attr.swipeRefreshForeground)
            )
        }.onSuccess { foregroundColor ->
            setColorSchemeColors(foregroundColor)
        }

        a.recycle()
    }
}
