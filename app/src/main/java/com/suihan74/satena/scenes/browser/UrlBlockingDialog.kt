package com.suihan74.satena.scenes.browser

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.WindowManager
import androidx.core.text.buildSpannedString
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleUrlBlockingBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.append
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

class UrlBlockingDialog : DialogFragment() {
    companion object {
        fun createInstance(urls: List<ResourceUrl>? = null) = UrlBlockingDialog().withArguments {
            putObject(ARG_URLS, urls)
        }

        private const val ARG_URLS = "ARG_URLS"
    }

    private val viewModel by lazyProvideViewModel {
        val urls = requireArguments().getObject<List<ResourceUrl>>(ARG_URLS)
        DialogViewModel(urls)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleViewBinding = DataBindingUtil.inflate<DialogTitleUrlBlockingBinding>(
            localLayoutInflater(),
            R.layout.dialog_title_url_blocking,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner ?: requireActivity()
        }

        val builder = createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_register, null)
            .setNeutralButton(R.string.dialog_url_blocking_continuous_register, null)
            .also { builder ->
                if (!viewModel.urls.isNullOrEmpty()) {
                    builder.setItems(viewModel.labels, null)
                }
            }

        return builder.show()
            .apply {
                // IMEを表示するための設定
                window?.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }

                // クリックしたURLをEditTextに入力する
                listView?.setOnItemClickListener { _, _, i, _ ->
                    viewModel.pattern.value = viewModel.getPatternCandidate(i)
                }

                // 既にブロックされている設定の説明を表示する
                listView?.setOnItemLongClickListener { _, _, i, _ ->
                    if (viewModel.urls?.get(i)?.blocked == true) {
                        showToast(R.string.msg_url_blocked)
                    }
                    true
                }

                // 一件だけ追加して閉じる
                getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                    if (invokeRegister()) {
                        dismiss()
                    }
                }

                // 連続追加
                getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                    if (invokeRegister()) {
                        viewModel.pattern.value = ""
                    }
                }
            }
    }

    /** 空白確認して設定を登録する */
    private fun invokeRegister() : Boolean {
        return if (viewModel.pattern.value.isNullOrBlank()) {
            showToast(R.string.msg_empty_url_blocking_pattern)
            false
        }
        else {
            viewModel.invokeOnComplete()
            true
        }
    }

    // ------ //

    fun setOnCompleteListener(
        listener: Listener<BlockUrlSetting>?
    ) = lifecycleScope.launchWhenCreated {
        viewModel.onComplete = listener
    }

    // ------ //

    class DialogViewModel(
        val urls: List<ResourceUrl>?
    ) : ViewModel() {
        var onComplete : Listener<BlockUrlSetting>? = null

        /** 候補リストの表示項目 */
        val labels by lazy {
            urls?.map {
                if (it.blocked) buildSpannedString {
                        append(
                            Uri.decode(it.url),
                            ForegroundColorSpan(Color.GRAY)
                        )
                    }
                else Uri.decode(it.url)
            }?.toTypedArray() ?: emptyArray()
        }

        /** 登録するパターン */
        val pattern by lazy {
            MutableLiveData("")
        }

        /** 正規表現として扱う */
        val isRegex by lazy {
            MutableLiveData(false)
        }

        // ------ //

        private val domainRegex : Regex by lazy {
            Regex("""^https?://([^/]+)""")
        }

        /** 選択したURLのドメイン部分をパターン候補として返す */
        fun getPatternCandidate(which: Int) : String {
            val url = urls?.get(which)?.url ?: return ""
            val match = domainRegex.find(url)

            return match?.groupValues?.getOrNull(1) ?: url
        }

        fun invokeOnComplete() {
            val setting = BlockUrlSetting(
                pattern.value ?: "",
                isRegex.value ?: false
            )
            onComplete?.invoke(setting)
        }
    }
}
