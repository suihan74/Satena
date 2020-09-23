package com.suihan74.satena.scenes.browser

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ToolbarBrowserBinding
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.whenTrue

class BrowserToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleId: Int = 0
) : Toolbar(context, attrs, defStyleId) {

    private var binding: ToolbarBrowserBinding? = null

    fun inflate(
        viewModel: BrowserViewModel,
        lifecycleOwner: LifecycleOwner,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) : ToolbarBrowserBinding {
        this.binding?.unbind()

        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<ToolbarBrowserBinding>(
            inflater,
            R.layout.toolbar_browser,
            parent,
            attachToParent
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = lifecycleOwner
        }

        binding.addressEditText.let { editText ->
            // フォーカスを失ったときに空欄だったら表示中のページのURLをセットし直す
            editText.setOnFocusChangeListener { view, b ->
                if (!b && viewModel.addressText.value.isNullOrBlank()) {
                    viewModel.addressText.value = viewModel.url.value ?: ""
                }
            }

            // IMEの決定ボタンでページ遷移する
            editText.setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_GO ->
                        viewModel.goAddress().whenTrue {
                            context.alsoAs<Activity> { activity ->
                                activity.hideSoftInputMethod(binding.toolbar)
                            }
                        }

                    else -> false
                }
            }
        }

        binding.favoriteButton.let { favButton ->
            // アドレステキストが表示中のURL以外になったらお気に入りボタンを隠す
            viewModel.addressText.observe(lifecycleOwner) {
                favButton.setVisibility(it == Uri.decode(viewModel.url.value))
            }

            viewModel.isUrlFavorite.observe(lifecycleOwner) {
                favButton.setImageResource(
                    if (it) R.drawable.ic_star
                    else R.drawable.ic_star_outline
                )

                TooltipCompat.setTooltipText(
                    favButton,
                    if (it) context?.getString(R.string.browser_favorite_button_tooltip)
                    else context?.getString(R.string.browser_unfavorite_button_tooltip)
                )
            }

            favButton.setOnClickListener {
                if (viewModel.isUrlFavorite.value == true) {
                    viewModel.unfavoriteCurrentPage()
                }
                else {
                    viewModel.favoriteCurrentPage()
                }
            }
        }

        this.binding = binding
        return binding
    }
}
