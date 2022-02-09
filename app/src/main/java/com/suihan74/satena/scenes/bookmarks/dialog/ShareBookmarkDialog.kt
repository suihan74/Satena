package com.suihan74.satena.scenes.bookmarks.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogShareBookmarkBinding
import com.suihan74.satena.dialogs.ShareDialogViewModel
import com.suihan74.utilities.exceptions.EmptyException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
        val entry = MutableStateFlow<Entry?>(null)

        val bookmark = MutableStateFlow<Bookmark?>(null)

        // ------ //

        private val _commentPageUrl = MutableStateFlow("")
        val commentPageUrl : StateFlow<String> = _commentPageUrl

        init {
            viewModelScope.launch {
                entry
                    .combine(bookmark) { entry, bookmark ->
                        if (entry == null || bookmark == null) ""
                        else bookmark.getCommentPageUrl(entry)
                    }
                    .collect { _commentPageUrl.value = it }
            }
        }

        // ------ //

        /**
         * ブクマへのリンクを「共有」する
         */
        fun shareLinkUrlString(fragment: ShareBookmarkDialog) {
            runCatching {
                shareText(fragment, commentPageUrl.value)
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
                if (commentPageUrl.value.isBlank()) throw EmptyException()
                copyTextToClipboard(fragment, commentPageUrl.value)
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
                if (commentPageUrl.value.isBlank()) throw EmptyException()
                openLinkInBrowser(fragment, commentPageUrl.value)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }
    }
}
