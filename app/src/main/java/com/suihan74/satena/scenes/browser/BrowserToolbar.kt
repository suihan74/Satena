package com.suihan74.satena.scenes.browser

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ToolbarBrowserBinding
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.hideSoftInputMethod
import com.suihan74.utilities.whenTrue

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

        // IMEの決定ボタンでページ遷移する
        binding.addressEditText.setOnEditorActionListener { _, action, _ ->
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

        this.binding = binding
        return binding
    }
}
