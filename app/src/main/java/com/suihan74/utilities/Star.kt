package com.suihan74.utilities

import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R

/** スターカラーごとの「★」テキスト表示用のスタイルを取得する */
val Star.styleId: Int
    get() = when(this.color) {
        StarColor.Yellow -> R.style.StarSpan_Yellow
        StarColor.Red -> R.style.StarSpan_Red
        StarColor.Green -> R.style.StarSpan_Green
        StarColor.Blue -> R.style.StarSpan_Blue
        StarColor.Purple -> R.style.StarSpan_Purple
    }
