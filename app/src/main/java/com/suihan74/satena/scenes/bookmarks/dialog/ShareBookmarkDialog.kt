package com.suihan74.satena.scenes.bookmarks.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogShareBookmarkBinding
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication
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

    class DialogViewModel : ViewModel() {
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
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        fun shareComment(fragment: ShareBookmarkDialog) {
            runCatching {
                shareText(fragment, bookmark.value!!.comment)
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
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        /**
         * リンクをブラウザで開く
         */
        fun openLink(fragment: ShareBookmarkDialog) {
            runCatching {
                val url = commentPageUrl
                val context = fragment.requireContext()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fragment.startActivity(
                    intent.createIntentWithoutThisApplication(context, url)
                )
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        // ------ //

        private fun shareText(fragment: ShareBookmarkDialog, text: CharSequence) {
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).also {
                    it.putExtra(Intent.EXTRA_TEXT, text)
                    it.type = "text/plain"
                }
                fragment.startActivity(
                    Intent.createChooser(intent, text)
                )
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }

        private fun copyTextToClipboard(fragment: ShareBookmarkDialog, text: CharSequence) {
            runCatching {
                val cm = fragment.requireContext().getSystemService(ClipboardManager::class.java)!!
                cm.setPrimaryClip(ClipData.newPlainText("", text))
                fragment.showToast(R.string.msg_copy_to_clipboard, text)
                fragment.dismiss()
            }.onFailure {
                SatenaApplication.instance.showToast(R.string.share_bookmark_failure)
            }
        }
    }
}
