package com.suihan74.satena.scenes.post

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.utilities.*
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class BookmarkPostFragment : CoroutineScopeFragment(), AlertDialogFragment.Listener {

    interface ResultListener {
        fun onPostBookmark(result: BookmarkResult)
    }

    private lateinit var mEntry: Entry
    private var mBookmarksEntry: BookmarksEntry? = null

    private var mRoot: View? = null
    val root: View
        get() = mRoot!!

    companion object {
        fun createInstance(entry: Entry, bookmarksEntry: BookmarksEntry? = null, initialVisibility: Int = View.VISIBLE) = BookmarkPostFragment().apply {
            mEntry = entry
            mBookmarksEntry = bookmarksEntry
            arguments = Bundle().apply {
                putInt(ARG_INITIAL_VISIBILITY, initialVisibility)
            }
        }

        private const val ARG_INITIAL_VISIBILITY = "initialVisibility"
        private const val BUNDLE_EDITING_COMMENT = "editingComment"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mRoot != null) {
            outState.putString(
                BUNDLE_EDITING_COMMENT,
                root.findViewById<EditText>(R.id.post_bookmark_comment).text.toString()
            )
        }
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

        savedInstanceState?.let {
            it.getString(BUNDLE_EDITING_COMMENT)?.let { comment ->
                root.findViewById<EditText>(R.id.post_bookmark_comment).setText(comment)
            }
        }

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

        val visibility = arguments?.getInt(ARG_INITIAL_VISIBILITY) ?: View.VISIBLE
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

            // 確認ダイアログを表示する
            if (isConfirmationDialogEnabled) {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.confirm_dialog_title_simple)
                    .setMessage(R.string.confirm_post_bookmark)
                    .setIcon(R.drawable.ic_baseline_help)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setPositiveButton(R.string.dialog_ok)
                    .show(childFragmentManager, "confirm_post_dialog")
            }
            else {
                launchPostBookmark()
            }
        }
        // コメント文字数によって投稿可能かどうかを判定する
        val validCounterColor = context!!.getThemeColor(R.attr.textColor)
        val invalidCounterColor = Color.rgb(0xff, 0x22, 0x22)
        commentEditor.addTextChangedListener(object : TextWatcher {
            private val tagRegex = Regex("""\[[^%/:\[\]]+]""")

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文字数を数える
                // どうやら文字のバイト数をもとに計算しているっぽいので可能な限り再現する
                val text = commentEditor.text.toString()
                val textLength = ceil(text.replace(tagRegex, "").sumBy { c ->
                    val code = c.toInt()
                    when (code / 255) {
                        0 -> 1
                        1 -> if (code <= 0xc3bf) 1 else 3
                        else -> 3
                    }
                } / 3f).toInt()

                commentCounter.text = textLength.toString()
                bookmarkButton.isEnabled = textLength <= 100

                commentCounter.setTextColor(if (bookmarkButton.isEnabled) {
                    validCounterColor
                }
                else {
                    invalidCounterColor
                })
            }
        })

        // タグサジェストリストの初期化
        initializeTagsList()
    }

    // 既にブコメを付けている場合その内容を反映する
    private fun setExistedComment() {
        root.findViewById<EditText>(R.id.post_bookmark_comment).apply {
            if (text.isBlank()) {
                val comment = if (mEntry.bookmarkedData != null) {
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
            }
        }
    }

    // タグサジェストリストの初期化
    private fun initializeTagsList() {
        val list = root.findViewById<RecyclerView>(R.id.tags_list).apply {
            visibility = View.GONE
        }

        launch(Dispatchers.Main) {
            val tags = HatenaClient.getUserTagsAsync().await()
            if (tags.isEmpty()) return@launch

            list.run {
                visibility = View.VISIBLE
                layoutManager = LinearLayoutManager(context).apply {
                    orientation = LinearLayoutManager.HORIZONTAL
                }
                adapter = object : PostTagsListAdapter(tags.map { it.text }) {
                    override fun onItemClicked(tag: String) {
                        val tagText = "[$tag]"
                        val tagRegex = Regex("""\[.+]""")
                        val editor = root.findViewById<EditText>(R.id.post_bookmark_comment)
                        if (!editor.text.contains(tagText)) {
                            val matches = tagRegex.findAll(editor.text)
                            val lastExisted = matches.lastOrNull()
                            val pos = lastExisted?.range?.endInclusive?.plus(1) ?: 0
                            editor.text.insert(pos, tagText)
                        }
                    }
                }
            }
        }
    }

    private fun launchPostBookmark() = launch(Dispatchers.Main) {
        val context = context ?: return@launch

        val commentEditor = root.findViewById<EditText>(R.id.post_bookmark_comment)
        val bookmarkButton = root.findViewById<Button>(R.id.post_bookmark_button)

        val postMastodonButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_mastodon)
        val postTwitterButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_twitter)
        val postFacebookButton = root.findViewById<ToggleButton>(R.id.post_bookmark_post_facebook)
        val privateButton = root.findViewById<ToggleButton>(R.id.post_bookmark_private)

        val comment = commentEditor.text.toString()
        val postMastodon = postMastodonButton.isChecked

        try {
            val result = HatenaClient.postBookmarkAsync(
                url = mEntry.url,
                comment = comment,
                postTwitter = postTwitterButton.isChecked,
                postFacebook = postFacebookButton.isChecked,
                isPrivate = privateButton.isChecked
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
                val listener = parentFragment as? ResultListener ?: activity as? ResultListener
                listener?.onPostBookmark(result)

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

    override fun onClickPositiveButton(dialog: AlertDialogFragment) {
        launchPostBookmark()
    }

    override fun onClickNegativeButton(dialog: AlertDialogFragment) {
        val bookmarkButton = root.findViewById<Button>(R.id.post_bookmark_button)
        val commentEditor = root.findViewById<EditText>(R.id.post_bookmark_comment)

        bookmarkButton.isEnabled = true
        commentEditor.isEnabled = true
    }
}
