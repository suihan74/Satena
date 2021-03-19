package com.suihan74.satena.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication

abstract class ShareDialogViewModel : ViewModel() {
    /**
     * 文字列を`ACTION_SEND`で「共有」する
     *
     * @throws TaskFailureException 処理失敗
     */
    fun shareText(fragment: Fragment, text: CharSequence) {
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).also {
                it.putExtra(Intent.EXTRA_TEXT, text)
                it.type = "text/plain"
            }
            fragment.startActivity(
                Intent.createChooser(intent, text)
            )
        }.onFailure {
            throw TaskFailureException(cause = it)
        }
    }

    /**
     * 文字列をクリップボードにコピーする
     *
     * @throws TaskFailureException 処理失敗
     */
    fun copyTextToClipboard(fragment: Fragment, text: CharSequence) {
        runCatching {
            val cm = fragment.requireContext().getSystemService(ClipboardManager::class.java)!!
            cm.setPrimaryClip(ClipData.newPlainText("", text))
            fragment.showToast(R.string.msg_copy_to_clipboard, text)
        }.onFailure {
            throw TaskFailureException(cause = it)
        }
    }

    /**
     * リンクをブラウザで開く
     */
    fun openLinkInBrowser(fragment: Fragment, url: String) {
        runCatching {
            val context = fragment.requireContext()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            fragment.startActivity(
                intent.createIntentWithoutThisApplication(context, url)
            )
        }.onFailure {
            throw TaskFailureException(cause = it)
        }
    }
}
