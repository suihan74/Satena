package com.suihan74.satena.scenes.browser

import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.text.buildSpannedString
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleUrlBlockingBinding
import com.suihan74.utilities.*

class UrlBlockingDialog : DialogFragment() {
    companion object {
        fun createInstance(urls: List<ResourceUrl>) = UrlBlockingDialog().withArguments {
            putObject(ARG_URLS, urls)
        }

        private const val ARG_URLS = "ARG_URLS"
    }

    private val viewModel : DialogViewModel by lazy {
        provideViewModel(this) {
            val urls = requireArguments().getObject<List<ResourceUrl>>(ARG_URLS) ?: emptyList()
            DialogViewModel(urls)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        val titleViewBinding = DataBindingUtil.inflate<DialogTitleUrlBlockingBinding>(
            inflater,
            R.layout.dialog_title_url_blocking,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = requireActivity()
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setItems(viewModel.labels, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_register) { _, _ ->
                viewModel.invokeOnComplete()
            }
            .show()
            .apply {
                listView.setOnItemClickListener { adapterView, view, i, l ->
                    viewModel.pattern.value = viewModel.labels[i].toString()
                }

                listView.setOnItemLongClickListener { adapterView, view, i, l ->
                    if (viewModel.urls[i].blocked) {
                        context.showToast(R.string.msg_url_blocked)
                    }
                    true
                }
            }
    }

    // ------ //

    suspend fun setOnCompleteListener(listener: Listener<BlockUrlSetting>?) = whenStarted {
        viewModel.onComplete = listener
    }

    // ------ //

    class DialogViewModel(
        val urls: List<ResourceUrl>
    ) : ViewModel() {
        var onComplete : Listener<BlockUrlSetting>? = null

        /** 候補リストの表示項目 */
        val labels by lazy {
            urls.map {
                if (it.blocked) buildSpannedString {
                        append(
                            Uri.decode(it.url),
                            ForegroundColorSpan(Color.GRAY)
                        )
                    }
                else Uri.decode(it.url)
            }.toTypedArray()
        }

        /** 登録するパターン */
        val pattern by lazy {
            MutableLiveData("")
        }

        /** 正規表現として扱う */
        val isRegex by lazy {
            MutableLiveData(false)
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
