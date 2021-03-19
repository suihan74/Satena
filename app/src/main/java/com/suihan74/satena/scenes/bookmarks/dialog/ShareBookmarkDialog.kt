package com.suihan74.satena.scenes.bookmarks.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogShareBookmarkBinding
import com.suihan74.satena.dialogs.ShareDialogViewModel
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel

class ShareBookmarkDialog : BottomSheetDialogFragment() {
    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = ShareBookmarkDialog().also { f ->
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.entry.value = entry
                f.viewModel.bookmark.value = bookmark
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
        val binding = FragmentDialogShareBookmarkBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.fragment = this
            it.lifecycleOwner = this
        }
        return binding.root
    }

    // ------ //

    class DialogViewModel : ShareDialogViewModel() {
        val entry = MutableLiveData<Entry>()

        val bookmark = MutableLiveData<Bookmark>()

        // ------ //

        val commentPageUrl : String
            get() = bookmark.value!!.getCommentPageUrl(entry.value!!)

        // ------ //

        /**
         * ブクマへのリンクを「共有」する
         */
        fun shareLinkUrlString(fragment: ShareBookmarkDialog) {
            runCatching {
                shareText(fragment, commentPageUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        fun shareComment(fragment: ShareBookmarkDialog) {
            runCatching {
                shareText(fragment, bookmark.value!!.comment)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        /**
         * ブクマへのリンクをクリップボードにコピーする
         */
        fun copyLinkUrlToClipboard(fragment: ShareBookmarkDialog) {
            runCatching {
                copyTextToClipboard(fragment, commentPageUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        /**
         * ブコメの内容をクリップボードにコピーする
         */
        fun copyCommentToClipboard(fragment: ShareBookmarkDialog) {
            runCatching {
                copyTextToClipboard(fragment, bookmark.value!!.comment)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        /**
         * リンクをブラウザで開く
         */
        fun openLink(fragment: ShareBookmarkDialog) {
            runCatching {
                openLinkInBrowser(fragment, commentPageUrl)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }
    }
}
