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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBookmarkPost2Binding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.post2.dialog.ConfirmPostBookmarkDialog
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_bookmark_post_2.*


class BookmarkPostActivity :
    AppCompatActivity(),
    ConfirmPostBookmarkDialog.Listener
{
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

        // Dialog tags
        /** 投稿確認ダイアログ */
        private const val DIALOG_CONFIRM_POST_BOOKMARK = "DIALOG_CONFIRM_POST_BOOKMARK"
    }

    private lateinit var viewModel: ViewModel
    private lateinit var binding: ActivityBookmarkPost2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        // テーマの設定
        setTheme(
            if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppDialogTheme_Dark
            else R.style.AppDialogTheme_Light
        )

        // Extraの取得
        val entry = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry
        val editingComment = intent.getStringExtra(EXTRA_EDITING_COMMENT)
        val entryUrl =
            when (intent.action) {
                Intent.ACTION_SEND ->
                    intent.getStringExtra(Intent.EXTRA_TEXT)

                Intent.ACTION_VIEW ->
                    intent.dataString

                else -> null
            }

        // ViewModelの生成/取得
        val factory = ViewModel.Factory(
            HatenaClient,
            AccountLoader(
                context = this,
                client = HatenaClient,
                mastodonClientHolder = MastodonClientHolder
            )
        )
        viewModel = ViewModelProvider(this, factory)[ViewModel::class.java]

        // データバインド
        binding = DataBindingUtil.setContentView<ActivityBookmarkPost2Binding>(
            this,
            R.layout.activity_bookmark_post_2
        ).apply {
            lifecycleOwner = this@BookmarkPostActivity
            vm = viewModel
        }

        when {
            entry != null ->
                viewModel.init(entry, editingComment, onError = onAuthError)

            !entryUrl.isNullOrBlank() ->
                viewModel.init(entryUrl, editingComment, onError = onAuthError)

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
                        postBookmark()
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
        val tagsListAdapter = object : TagsListAdapter() {
            override fun onItemClicked(tag: String) {
                try {
                    viewModel.toggleTag(tag)
                }
                catch (e: ViewModel.TooManyTagsException) {
                    showToast(R.string.msg_post_too_many_tags)
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
            postBookmark()
        }

        // 利用タグ情報をロード完了したらリストに反映する
        viewModel.tags.observe(this, Observer {
            tagsListAdapter.setTags(
                it.map { t -> t.text }
            )
        })

        // 各トグルボタンをONにしたときにメッセージを表示する
        viewModel.postMastodon.observe(this, Observer {
            if (it) showToast(R.string.hint_mastodon_toggle)
        })

        viewModel.postTwitter.observe(this, Observer {
            if (it) showToast(R.string.hint_twitter_toggle)
        })

        viewModel.postFacebook.observe(this, Observer {
            if (it) showToast(R.string.hint_facebook_toggle)
        })

        viewModel.isPrivate.observe(this, Observer {
            if (it) showToast(R.string.hint_private_toggle)
        })
    }

    /** 初期化失敗時処理 */
    private val onAuthError: OnError = { e ->
        when (e) {
            is AccountLoader.MastodonSignInException -> {
                showToast(R.string.msg_auth_mastodon_failed)
            }

            is ViewModel.LoadingTagsFailureException -> {
                showToast("タグリストを取得できませんでした")
            }

            else -> {
                showToast(R.string.msg_auth_failed)
                finish()
            }
        }
        Log.e("InitPostActivity", Log.getStackTraceString(e))
    }

    /** 投稿成功時処理 */
    private val onPostSuccess = { result: BookmarkResult ->
        showToast(R.string.msg_post_bookmark_succeeded)
        val intent = Intent().apply {
            putExtra(RESULT_BOOKMARK, result)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    /** 投稿失敗時処理 */
    private val onPostError: OnError = { e ->
        when (e) {
            is ViewModel.CommentTooLongException ->
                showToast(R.string.msg_comment_too_long, ViewModel.MAX_COMMENT_LENGTH)

            is ViewModel.TooManyTagsException ->
                showToast(R.string.msg_post_too_many_tags)

            else ->
                showToast(R.string.msg_post_bookmark_failed)
        }
    }

    /** ブクマ投稿を実際に送信する */
    private fun postBookmarkImpl() {
        hideSoftInputMethod()
        comment.isEnabled = false
        post_button.isEnabled = false
        tags_list.isEnabled = false

        viewModel.postBookmark(
            onSuccess = onPostSuccess,
            onError = onPostError,
            onFinally = { e ->
                if (e != null) {
                    comment.isEnabled = true
                    post_button.isEnabled = true
                    tags_list.isEnabled = true
                }
            }
        )
    }

    /** ブックマークを投稿する（必要ならダイアログを表示する） */
    private fun postBookmark() {
        if (!viewModel.checkCommentLength(onPostError)) return

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val showDialog = prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)
        if (showDialog) {
            ConfirmPostBookmarkDialog.createInstance(viewModel)
                .show(supportFragmentManager, DIALOG_CONFIRM_POST_BOOKMARK)
        }
        else {
            postBookmarkImpl()
        }
    }

    override fun onApprovedToPost(dialog: ConfirmPostBookmarkDialog) {
        when (dialog.tag) {
            DIALOG_CONFIRM_POST_BOOKMARK -> postBookmarkImpl()
        }
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
