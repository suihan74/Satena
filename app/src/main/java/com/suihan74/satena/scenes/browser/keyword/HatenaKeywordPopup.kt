package com.suihan74.satena.scenes.browser.keyword

import android.content.Context
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.core.text.buildSpannedString
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.satena.databinding.PopupHatenaKeywordBinding
import com.suihan74.utilities.append
import com.suihan74.utilities.dp2px
import com.suihan74.utilities.sp2px

/** キーワードの解説を表示するポップアップ */
class HatenaKeywordPopup(
    context: Context,
    data: List<Keyword>
) : PopupWindow(context, null, 0) {
    init {
        val firstItem = data.first()

        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<PopupHatenaKeywordBinding>(
            inflater,
            R.layout.popup_hatena_keyword,
            null,
            false
        ).apply {
            title = buildSpannedString {
                append(firstItem.title)
                append(firstItem.kana, AbsoluteSizeSpan(context.sp2px(13)))
            }
            lifecycleOwner = context as? LifecycleOwner
        }

        binding.recyclerView.also {
            it.adapter = HatenaKeywordAdapter(data)
        }

        val view = binding.root

        contentView = view
        width = context.dp2px(200)
        height = context.dp2px(200)
        isFocusable = true
        isTouchable = true
        elevation = context.dp2px(8).toFloat()
    }
}
