package com.suihan74.satena.scenes.entries2.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogShareEntryBinding
import com.suihan74.satena.dialogs.ShareDialogViewModel
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel

class ShareEntryDialog : BottomSheetDialogFragment() {

    companion object {
        fun createInstance(entry: Entry) = ShareEntryDialog().also { f ->
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.entry.value = entry
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
        var entry = MutableLiveData<Entry>()

        // ------ //

        fun copyUrlToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, entry.value!!.url)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun openUrl(fragment: DialogFragment) {
            runCatching {
                openLinkInBrowser(fragment, entry.value!!.url)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareUrl(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, entry.value!!.url)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        // ------ //

        val entryUrl
            get() = HatenaClient.getCommentPageUrlFromEntryUrl(entry.value!!.url)

        fun copyEntryUrlToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, entryUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun openEntryUrl(fragment: DialogFragment) {
            runCatching {
                openLinkInBrowser(fragment, entryUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareEntryUrl(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, entryUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        // ------ //

        val text
            get() = entry.value!!.let { it.title + " " + it.url }

        fun copyTitleToClipboard(fragment: DialogFragment) {
            runCatching {
                copyTextToClipboard(fragment, text)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }

        fun shareTitle(fragment: DialogFragment) {
            runCatching {
                shareText(fragment, text)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_entry_failure)
            }
        }
    }
}
