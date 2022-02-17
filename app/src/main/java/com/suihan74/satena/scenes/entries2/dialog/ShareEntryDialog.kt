package com.suihan74.satena.scenes.entries2.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogShareEntryBinding
import com.suihan74.satena.dialogs.ShareDialogViewModel
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShareEntryDialog : BottomSheetDialogFragment() {

    companion object {
        fun createInstance(entry: Entry) = ShareEntryDialog().also { f ->
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.url.value = entry.url
                f.viewModel.title.value = entry.title
            }
        }

        fun createInstance(url: String, title: String?) = ShareEntryDialog().also { f ->
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.url.value = url
                f.viewModel.title.value =
                    if (url == title) null
                    else title
            }
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel()
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDialogShareEntryBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.fragment = this
            it.lifecycleOwner = this
        }
        return binding.root
    }

    // ------ //

    class DialogViewModel : ShareDialogViewModel() {
        val url = MutableStateFlow("")

        val title = MutableStateFlow<String?>(null)

        private val entryUrl = MutableStateFlow("")

        val text = MutableStateFlow("")

        init {
            viewModelScope.launch {
                url.collect {
                    entryUrl.value =
                        if (it.isBlank()) ""
                        else HatenaClient.getCommentPageUrlFromEntryUrl(it)
                }
            }

            url
                .combine(title) { url, title ->
                    title?.let {
                        buildString { append(it, " ", url) }
                    }.orEmpty()
                }
                .onEach { text.value = it }
                .launchIn(viewModelScope)
        }

        // ------ //

        fun copyUrlToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, url.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun openUrl(fragment: DialogFragment) {
            runCatching {
                openLinkInBrowser(fragment, url.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareUrl(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, url.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        // ------ //

        fun copyEntryUrlToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, entryUrl.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun openEntryUrl(fragment: DialogFragment) {
            runCatching {
                openLinkInBrowser(fragment, entryUrl.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareEntryUrl(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, entryUrl.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        // ------ //

        fun copyTitleToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, text.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareTitle(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, text.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }
    }
}
