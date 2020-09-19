package com.suihan74.satena.dialogs

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.ParcelableSpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.extensions.withArguments
import kotlinx.android.synthetic.main.fragment_dialog_release_notes.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReleaseNotesDialogFragment : DialogFragment() {
    companion object {
        fun createInstance() = ReleaseNotesDialogFragment()

        fun createInstance(
            lastVersionName: String,
            currentVersionName: String
        ) = ReleaseNotesDialogFragment().withArguments {
            putString(ARG_LAST_VERSION_NAME, lastVersionName)
            putString(ARG_CURRENT_VERSION_NAME, currentVersionName)
        }

        /** 最後に起動したときのバージョン */
        private const val ARG_LAST_VERSION_NAME = "ARG_LAST_VERSION_NAME"

        /** 現在実行中のバージョン */
        private const val ARG_CURRENT_VERSION_NAME = "ARG_CURRENT_VERSION_NAME"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val view = activity.layoutInflater.inflate(R.layout.fragment_dialog_release_notes, null)
        val titleColor = ContextCompat.getColor(activity, R.color.colorPrimary)

        val lastVersionName = arguments?.getString(ARG_LAST_VERSION_NAME)
        val currentVersionName = arguments?.getString(ARG_CURRENT_VERSION_NAME)

        if (lastVersionName != null && currentVersionName != null) {
            view.message.text = requireContext().getString(R.string.release_notes_dialog_update_message, lastVersionName, currentVersionName)
            view.message.visibility = View.VISIBLE
        }
        else {
            view.message.visibility = View.GONE
        }

        // 履歴の読み込み
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                resources.openRawResource(R.raw.release_notes).bufferedReader().use { reader ->
                    // 最後の起動時のバージョンが渡されている場合、そこから最新までの差分だけを表示する
                    val text = when (lastVersionName) {
                        null -> reader.readText()
                        else -> buildString {
                            reader.useLines { it.forEach { line ->
                                if (line.contains("[ version $lastVersionName ]")) {
                                    return@useLines
                                }
                                else {
                                    append(line, "\n")
                                }
                            } }
                        }
                    }

                    val builder = SpannableStringBuilder(text)
                    val titleRegex = Regex("""\[\s*version\s*\S+\s*]""")

                    // 各バージョンのタイトル部分を強調表示
                    titleRegex.findAll(text).forEach {
                        builder.apply {
                            setSpan(StyleSpan(Typeface.BOLD), it)
                            setSpan(ForegroundColorSpan(titleColor), it)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        view.text_view.text = builder
                    }
                }
            }
            catch (e: Throwable) {
                activity.showToast(R.string.msg_read_release_notes_failed)
            }
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.release_notes_dialog_title)
            .setNegativeButton(R.string.dialog_close, null)
            .setView(view)
            .create()
    }

    /** 正規表現にマッチした部分を装飾する */
    private fun SpannableStringBuilder.setSpan(span: ParcelableSpan, match: MatchResult) = setSpan(
        span,
        match.range.first,
        match.range.last + 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}
