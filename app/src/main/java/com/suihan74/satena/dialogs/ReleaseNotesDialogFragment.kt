package com.suihan74.satena.dialogs

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.ParcelableSpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.R
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ReleaseNotesDialogFragment : DialogFragment(), CoroutineScope {
    private val mJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = mJob

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    private var mRoot: View? = null

    companion object {
        fun createInstance() = ReleaseNotesDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_release_notes, null)
        mRoot = content
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val titleColor = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)

        // 履歴の読み込み
        launch(Dispatchers.IO) {
            try {
                resources.openRawResource(R.raw.release_notes).bufferedReader().use { reader ->
                    val titleRegex = Regex("""\[\s*version\s*\S+\s*]""")
                    val text = reader.readText()
                    val builder = SpannableStringBuilder(text)
                    // 各バージョンのタイトル部分を強調表示
                    titleRegex.findAll(text).forEach {
                        builder.apply {
                            setSpan(StyleSpan(Typeface.BOLD), it)
                            setSpan(ForegroundColorSpan(titleColor), it)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        content.findViewById<TextView>(R.id.text_view).text = builder
                    }
                }
            }
            catch (e: Exception) {
                activity?.showToast("更新履歴の読み込み失敗")
            }
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setView(content)
            .setTitle("更新履歴")
            .setNegativeButton("閉じる", null)
            .create()
    }

    /** 正規表現にマッチした部分を装飾する */
    private fun SpannableStringBuilder.setSpan(span: ParcelableSpan, match: MatchResult) = setSpan(
        span,
        match.range.first,
        match.range.last + 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}
