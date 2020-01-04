package com.suihan74.satena.scenes.post2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBookmarkPost2Binding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.post2.dialog.ConfirmPostBookmarkDialog
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.activity_bookmark_post_2.*

class BookmarkPostActivity :
    AppCompatActivity(),
    ConfirmPostBookmarkDialog.Listener
{
    companion object {
        const val EXTRA_ENTRY = "BookmarkPostActivity.EXTRA_ENTRY"

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
        viewModel = ViewModelProviders.of(this, factory)[ViewModel::class.java]

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
                viewModel.init(entry, onError = onAuthError)

            !entryUrl.isNullOrBlank() ->
                viewModel.init(entryUrl, onError = onAuthError)

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
                        /* DONEボタン押したときの処理 */
                        true
                    }
                    else -> false
                }
            }

            // 画面開くと同時にフォーカスする
            if (savedInstanceState == null) {
                requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, 0)
            }
        }

        // タグリストを初期化
        val tagsListAdapter = object : TagsListAdapter() {
            override fun onItemClicked(tag: String) {
                viewModel.toggleTag(tag)
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
            tagsListAdapter.setTags(it.map { t -> t.text })
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
    private val onPostSuccess = {
        showToast(R.string.msg_post_bookmark_succeeded)
        finish()
    }

    /** 投稿失敗時処理 */
    private val onPostError: OnError = { e ->
        when (e) {
            is ViewModel.CommentTooLongException -> {
                showToast(R.string.msg_comment_too_long, ViewModel.MAX_COMMENT_LENGTH)
            }

            else -> {
                showToast(R.string.msg_post_bookmark_failed)
            }
        }
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
            viewModel.postBookmark(
                onSuccess = onPostSuccess,
                onError = onPostError
            )
        }
    }

    override fun onApprovedToPost(dialog: ConfirmPostBookmarkDialog) {
        when (dialog.tag) {
            DIALOG_CONFIRM_POST_BOOKMARK ->
                viewModel.postBookmark(
                    onSuccess = onPostSuccess,
                    onError = onPostError
                )
        }
    }
}
