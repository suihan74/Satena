package com.suihan74.satena.scenes.post2

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBookmarkPost2Binding
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.*
import kotlinx.android.synthetic.main.activity_bookmark_post_2.*

class BookmarkPostActivity : AppCompatActivity() {
    companion object {
        // Extra keys
        /** ブクマ対象のエントリ */
        const val EXTRA_ENTRY = "BookmarkPostActivity.EXTRA_ENTRY"

        /** 初期表示するコメント */
        const val EXTRA_EDITING_COMMENT = "BookmarkPostActivity.EXTRA_EDITING_COMMENT"

        /** 呼び出し元がBookmarksActivityである */
        const val EXTRA_INVOKED_BY_BOOKMARKS_ACTIVITY = "BookmarkPostActivity.EXTRA_INVOKED_FROM_BOOKMARKS_ACTIVITY"

        // Request codes
        val REQUEST_CODE
            get() = hashCode() and 0x0000ffff

        // Result keys
        /** 成功時: 投稿完了したブクマ情報(BookmarkResult)を返す */
        const val RESULT_BOOKMARK = "BookmarkPostActivity.RESULT_BOOKMARK"
        /** 失敗時: 編集途中のコメントを返す */
        const val RESULT_EDITING_COMMENT = "BookmarkPostActivity.RESULT_EDITING_COMMENT"
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            BookmarkPostViewModel(
                HatenaClient,
                AccountLoader(
                    context = this,
                    client = HatenaClient,
                    mastodonClientHolder = MastodonClientHolder
                ),
                SafeSharedPreferences.create(this)
            ).apply {
                displayEntryTitle.value =
                    !intent.getBooleanExtra(EXTRA_INVOKED_BY_BOOKMARKS_ACTIVITY, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // テーマの設定
        setTheme(viewModel.themeId)

        // Extraの取得
        val entry = intent.getObjectExtra<Entry>(EXTRA_ENTRY)
        val editingComment = intent.getStringExtra(EXTRA_EDITING_COMMENT)
        val entryUrl = intent.getStringExtra(Intent.EXTRA_TEXT)

        // データバインド
        DataBindingUtil.setContentView<ActivityBookmarkPost2Binding>(
            this,
            R.layout.activity_bookmark_post_2
        ).apply {
            lifecycleOwner = this@BookmarkPostActivity
            vm = viewModel
        }

        when {
            entry != null ->
                viewModel.init(entry, editingComment, onError = onInitError)

            !entryUrl.isNullOrBlank() ->
                viewModel.init(entryUrl, editingComment, onError = onInitError)

            else -> {
                showToast(R.string.msg_get_entry_information_failed)
                finish()
            }
        }

        // EditTextの設定
        comment.run {
            // 注: XML側に書くと折り返さない一行での表示になる
            setHorizontallyScrolling(false)
            // 注: XML側に書くと表示部分の縦幅が一行分だけになる
            maxLines = Int.MAX_VALUE

            // DONEボタンとかSEARCHボタンとかが押された時の処理
            setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_DONE -> {
                        hideSoftInputMethod()
                        viewModel.postBookmark(
                            supportFragmentManager,
                            onPostSuccess,
                            onPostError
                        )
                        true
                    }
                    else -> false
                }
            }

            // 画面開くと同時にフォーカスする
            if (savedInstanceState == null) {
                addTextChangedListener(object : TextWatcher {
                    var initialized = false
                    override fun afterTextChanged(s: Editable?) {
                        if (!initialized) {
                            setSelection(s?.length ?: 0)
                            initialized = true
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
                showSoftInputMethod(this@run)
            }
        }

        // タグリストを初期化
        val tagsListAdapter = TagsListAdapter().also { adapter ->
            adapter.setOnItemClickedListener { tag ->
                var watcher: TextWatcher? = null
                try {
                    watcher = object : TextWatcher {
                        private var before: Int = 0
                        private var countDiff: Int = 0
                        private var tagsEnd: Int = 0

                        override fun afterTextChanged(s: Editable?) {
                            val after = comment.selectionStart
                            if (after == 0) {
                                val selecting =
                                    if (before < tagsEnd) viewModel.getTagsEnd(s)
                                    else before + countDiff
                                comment.setSelection(selecting)
                            }
                            comment.removeTextChangedListener(watcher)
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            this.tagsEnd = viewModel.getTagsEnd(s)
                            this.before = comment.selectionStart
                            this.countDiff = after - count
                        }
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    }

                    comment.addTextChangedListener(watcher)
                    viewModel.toggleTag(tag)

                }
                catch (e: BookmarkPostViewModel.TooManyTagsException) {
                    showToast(R.string.msg_post_too_many_tags)
                    comment.removeTextChangedListener(watcher)
                }
            }
        }
        tags_list.run {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
            adapter = tagsListAdapter
        }

        // 投稿ボタン処理
        post_button.setOnClickListener {
            hideSoftInputMethod()
            viewModel.postBookmark(
                supportFragmentManager,
                onPostSuccess,
                onPostError
            )
        }

        // 利用タグ情報をロード完了したらリストに反映する
        viewModel.tags.observe(this) {
            tagsListAdapter.setTags(
                it.map { t -> t.text }
            )
        }

        // 各トグルボタンをONにしたときにメッセージを表示する
        viewModel.postMastodon.observe(this) {
            if (it) showToast(R.string.hint_mastodon_toggle)
        }

        viewModel.postTwitter.observe(this) {
            if (it) showToast(R.string.hint_twitter_toggle)
        }

        viewModel.postFacebook.observe(this) {
            if (it) showToast(R.string.hint_facebook_toggle)
        }

        viewModel.isPrivate.observe(this) {
            if (it) showToast(R.string.hint_private_toggle)
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.displayEntryTitle.value == true) {
            // エントリタイトル部分をマーキーするために必要
            entry_title.isSelected = true
        }
    }

    /** 初期化失敗時処理 */
    private val onInitError: OnError = { e ->
        when (e) {
            is AccountLoader.MastodonSignInException -> showToast(R.string.msg_auth_mastodon_failed)

            is BookmarkPostViewModel.LoadingTagsFailureException -> showToast(R.string.msg_fetch_tags_failed)

            is InvalidUrlException -> {
                showToast(R.string.invalid_url_error)
                finish()
            }

            is BookmarkPostViewModel.NotSignedInException -> {
                showToast(R.string.msg_hatena_not_signed_in)
                finish()
            }

            else -> {
                showToast(R.string.msg_auth_failed)
                finish()
            }
        }
        Log.e("InitPostActivity", Log.getStackTraceString(e))
    }

    /** 投稿成功時処理 */
    private val onPostSuccess: OnSuccess<BookmarkResult> = { result ->
        showToast(R.string.msg_post_bookmark_succeeded)
        val intent = Intent().apply {
            putObjectExtra(RESULT_BOOKMARK, result)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    /** 投稿失敗時処理 */
    private val onPostError: OnError = { e ->
        when (e) {
            is BookmarkPostViewModel.MultiplePostException ->
                showToast(R.string.msg_multiple_post)

            is BookmarkPostViewModel.CommentTooLongException ->
                showToast(R.string.msg_comment_too_long, BookmarkPostViewModel.MAX_COMMENT_LENGTH)

            is BookmarkPostViewModel.TooManyTagsException ->
                showToast(R.string.msg_post_too_many_tags)

            else ->
                showToast(R.string.msg_post_bookmark_failed)
        }
        Log.e("postBookmark", Log.getStackTraceString(e))
    }

    /** 戻るボタンで閉じる場合、編集中のコメントを保存しておく */
    override fun onBackPressed() {
        hideSoftInputMethod()
        val intent = Intent().apply {
            putExtra(RESULT_EDITING_COMMENT, viewModel.comment.value)
        }
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Activityの外側をタップして閉じる際に、結果を渡しておく
        if (event?.action == MotionEvent.ACTION_DOWN && isOutOfBounds(event)) {
            hideSoftInputMethod()
            val intent = Intent().apply {
                putExtra(RESULT_EDITING_COMMENT, viewModel.comment.value)
            }
            setResult(RESULT_CANCELED, intent)
        }
        return super.onTouchEvent(event)
    }

    /** Activityの外側をタップしたかを判別する */
    private fun isOutOfBounds(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        val dialogBounds = Rect()
        window.decorView.getHitRect(dialogBounds)
        return !dialogBounds.contains(x, y)
    }
}
