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
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.append
import com.suihan74.utilities.extensions.dp2px
import com.suihan74.utilities.extensions.sp2px

/** キーワードの解説を表示するポップアップ */
class HatenaKeywordPopup(
    val context: Context,
    data: List<Keyword>? = null
) : PopupWindow(context, null, 0) {

    private var binding : PopupHatenaKeywordBinding

    // ------ //

    init {
        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<PopupHatenaKeywordBinding>(
            inflater,
            R.layout.popup_hatena_keyword,
            null,
            false
        ).also {
            it.lifecycleOwner = context as? LifecycleOwner
        }
        this.binding = binding

        if (data != null) {
            setData(data)
        }
        else {
            binding.progressBar.setVisibility(true)
        }

        val view = binding.root

        contentView = view
        width = context.dp2px(200)
        height = context.dp2px(200)
        isFocusable = true
        isTouchable = true
        elevation = context.dp2px(8).toFloat()
    }

    // ------ //

    /** データを表示する */
    fun setData(data: List<Keyword>) {
        binding.progressBar.setVisibility(false)

        data.firstOrNull()?.let { firstItem ->
            binding.title = buildSpannedString {
                append(firstItem.title)
                append(firstItem.kana, AbsoluteSizeSpan(context.sp2px(13)))
            }
        }

        binding.recyclerView.also {
            it.adapter = HatenaKeywordAdapter(data)
        }
    }
}
