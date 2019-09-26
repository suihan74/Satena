package com.suihan74.satena.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.activities.BookmarkPostActivity
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkPostFragment : CoroutineScopeFragment() {

    private lateinit var mEntry: Entry
    private var mBookmarksEntry: BookmarksEntry? = null
    private var mOnPostedAction: ((BookmarkResult)->Unit)? = null

    private var mRoot: View? = null
    val root: View
        get() = mRoot!!

    companion object {
        const val INITIAL_VISIBILITY = "initial_visibility"

        fun createInstance(entry: Entry, bookmarksEntry: BookmarksEntry? = null) = BookmarkPostFragment().apply {
            mEntry = entry
            mBookmarksEntry = bookmarksEntry
        }

        private var savedComment : String? = null
    }

    fun setOnPostedListener(onPostedAction: ((BookmarkResult)->Unit)?) {
        mOnPostedAction = onPostedAction
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedComment = root.findViewById<EditText>(R.id.post_bookmark_comment).text.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = inflater.inflate(R.layout.fragment_bookmark_post, container, false)

        savedInstanceState?.let {
            val activity = activity
            mEntry = when (activity) {
                is BookmarksActivity -> activity.bookmarksFragment!!.entry
                is BookmarkPostActivity -> activity.entry
                else -> throw NotImplementedError()
            }
        }

        initBookmarkDialog()

        //retainInstance = true
        return mRoot
    }

    override fun onResume() {
        super.onResume()
        setExistedComment()
    }

    fun focus() {
        val editText = root.findViewById<EditText>(R.id.post_bookmark_comment)
        editText.requestFocus()
        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, 0)
    }

    fun unFocus() {
        val editText = root.findViewById<EditText>(R.id.post_bookmark_comment)
        val inputMethodManager =  context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, InputMethodManager.RESULT_UNCHANGED_SHOWN)
    }

    private fun initBookmarkDialog() {
        val commentEditor = root.findViewById<EditText>(R.id.post_bookmark_comment).apply {
            // 右端で自動折り返しはするが改行は受け付けない
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE

            // Doneボタン押下でIME隠す
            setOnEditorActionListener { _, action, _ ->
                val currentFocus = activity?.currentFocus ?: return@setOnEditorActionListener false
                when (action) {
                    EditorInfo.IME_ACTION_DONE -> {
                        val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        im.hideSoftInputFromWindow(currentFocus.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                        true
                    }
                    else -> false
                }
            }

            text.clear()
        }

        val commentCounter = root.findViewById<TextView>(R.id.post_bookmark_comment_count)
        val bookmarkButton = root.findViewById<Button>(R.id.post_bookmark_button)

        val postMastodonButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_mastodon)
        val postTwitterButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_twitter)
        val postFacebookButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_facebook)
        val postEvernoteButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_evernote)
        val privateButton = root.findViewById<ToggleButton>(R.id.post_bookmark_private)

        val visibility = arguments?.getInt(INITIAL_VISIBILITY) ?: View.VISIBLE
        root.findViewById<View>(R.id.bookmark_post_layout).visibility = visibility

        val account = HatenaClient.account!!
        if (!account.isOAuthTwitter) {
            postTwitterButton.visibility = View.GONE
        }
        if (!account.isOAuthFaceBook) {
            postFacebookButton.visibility = View.GONE
        }
        if (true/*!account.isOAuthEvernote*/) {
            postEvernoteButton.visibility = View.GONE
        }

        if (!MastodonClientHolder.signedIn()) {
            postMastodonButton.visibility = View.GONE
        }

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val isConfirmationDialogEnabled = prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)

        // ブックマークする
        bookmarkButton.setOnClickListener {
            bookmarkButton.isEnabled = false
            commentEditor.isEnabled = false

            val comment = commentEditor.text.toString()
            val postMastodon = postMastodonButton.isChecked
            val postTwitter = postTwitterButton.isChecked
            val private = privateButton.isChecked

            fun launchPostBookmark() = launch(Dispatchers.Main) {
                val context = context ?: return@launch
                try {
                    val result = HatenaClient.postBookmarkAsync(
                        url = mEntry.url,
                        comment = comment,
                        postTwitter = postTwitter,
                        isPrivate = private
                    ).await()

                    if (result.success != true) throw RuntimeException("failed to bookmark")

                    if (postMastodon) {
                        // Mastodonに投稿
                        val status =
                            if (result.comment.isBlank()) {
                                "\"${mEntry.title}\" ${mEntry.url}"
                            }
                            else {
                                "${result.comment} / \"${mEntry.title}\" ${mEntry.url}"
                            }

                        withContext(Dispatchers.IO) {
                            val client = MastodonClientHolder.client!!
                            Statuses(client).postStatus(
                                status = status,
                                inReplyToId = null,
                                sensitive = false,
                                visibility = Status.Visibility.Public,
                                mediaIds = null,
                                spoilerText = null
                            ).execute()
                        }
                    }

                    bookmarkButton.isEnabled = true
                    commentEditor.isEnabled = true
                    context.showToast("ブクマ登録完了")

                    try {
                        mOnPostedAction?.invoke(result)
                    }
                    catch (e: Exception) {
                        Log.e("OnPostedAction", e.message)
                    }
                }
                catch(e: Exception) {
                    Log.d("PostBookmark", Log.getStackTraceString(e))
                    bookmarkButton.isEnabled = true
                    commentEditor.isEnabled = true
                    context.showToast("ブクマ登録失敗")
                }
            }

            // 確認ダイアログを表示する
            if (isConfirmationDialogEnabled) {
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("確認")
                    .setMessage("本当にブックマークしますか？")
                    .setIcon(R.drawable.ic_baseline_help)
                    .setNegativeButton("Cancel") { _, _ ->
                        bookmarkButton.isEnabled = true
                        commentEditor.isEnabled = true
                    }
                    .setPositiveButton("OK") { _, _ -> launchPostBookmark() }
                    .show()
            }
            else {
                launchPostBookmark()
            }
        }
        // コメント文字数によって投稿可能かどうかを判定する
        val tagRegex = Regex("""\[.+]""")
        val validCounterColor = context!!.getThemeColor(R.attr.textColor)
        val invalidCounterColor = Color.rgb(0xff, 0x22, 0x22)
        commentEditor.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = commentEditor.text.toString()
                val rawText = text.replace(tagRegex, "")

                commentCounter.text = rawText.length.toString()
                bookmarkButton.isEnabled = rawText.length <= 100

                commentCounter.setTextColor(if (bookmarkButton.isEnabled) {
                    validCounterColor
                }
                else {
                    invalidCounterColor
                })
            }
        })
    }

    // 既にブコメを付けている場合その内容を反映する
    private fun setExistedComment() {
        root.findViewById<EditText>(R.id.post_bookmark_comment).apply {
            if (text.isBlank()) {
                val comment = if (savedComment != null) {
                    savedComment!!
                }
                else if (mEntry.bookmarkedData != null) {
                    mEntry.bookmarkedData!!.commentRaw
                }
                else if (mBookmarksEntry != null && HatenaClient.signedIn()) {
                    val b = mBookmarksEntry!!.bookmarks.firstOrNull { it.user == HatenaClient.account!!.name }
                    b?.comment ?: ""
                }
                else {
                    ""
                }

                text.clear()
                text.append(comment)
                savedComment = null
            }
        }
    }

}
