package com.suihan74.satena.scenes.post

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.scenes.post.BookmarkPostViewModelOwner.Companion.VIEW_MODEL_BOOKMARK_POST
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.lazyProvideViewModel

/**
 * ブクマ投稿用のモーダルウィンドウ
 */
class BookmarkPostActivity :
    AppCompatActivity(),
    BookmarkPostViewModelOwner
{
    companion object {
        /** ブクマ対象のエントリ */
        const val EXTRA_ENTRY = "BookmarkPostActivity.EXTRA_ENTRY"

        /** ブクマ対象のURL (エントリ情報がない場合に必要) */
        const val EXTRA_URL = "BookmarkPostActivity.EXTRA_URL"

        /** 復元させる編集データ */
        const val EXTRA_EDIT_DATA = "BookmarkPostActivity.EDIT_DATA"

        /** 結果を返すためのコード */
        val REQUEST_CODE
            get() = hashCode() and 0x0000ffff

        // ------ //
        // Result keys

        /** 対象のエントリ */
        const val RESULT_ENTRY = "BookmarkPostActivity.RESULT_ENTRY"

        /** キャンセル時: 編集データを返す */
        const val RESULT_EDIT_DATA = "BookmarkPostActivity.EDIT_DATA"

        /** 成功時: 投稿完了したブクマ情報(BookmarkResult)を返す */
        const val RESULT_BOOKMARK = "BookmarkPostActivity.RESULT_BOOKMARK"
    }

    // ------ //

    override val bookmarkPostViewModel by lazyProvideViewModel(VIEW_MODEL_BOOKMARK_POST) {
        val repository = BookmarkPostRepository(
            AccountLoader(
                context = this,
                client = HatenaClient,
                mastodonClientHolder = MastodonClientHolder
            ),
            SafeSharedPreferences.create(this)
        )

        BookmarkPostViewModel(repository)
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(bookmarkPostViewModel.themeId)
        setContentView(R.layout.activity_bookmark_post)

        if (supportFragmentManager.findFragmentById(R.id.main_layout) == null) {
            initializeViewModel()

            val fragment = BookmarkPostFragment.createInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_layout, fragment)
                .commitAllowingStateLoss()
        }
    }

    /** ViewModelに必要な情報を渡す */
    private fun initializeViewModel() {
        // 初期化失敗したらダイアログを閉じる
        bookmarkPostViewModel.setOnInitializeFailureListener {
            setCancelResult()
            finish()
        }

        // 投稿成功したらダイアログを閉じて結果を返す
        bookmarkPostViewModel.setOnPostSuccessListener { bookmarkResult ->
            setSuccessResult(bookmarkResult)
            finish()
        }

        // 復元する編集データ
        val editData = intent.getObjectExtra<BookmarkEditData>(EXTRA_EDIT_DATA)

        // ブクマ対象のエントリかURLをセットする
        // どちらも渡されていない場合はエラーである
        val entry = intent.getObjectExtra<Entry>(EXTRA_ENTRY)
        val url = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent.getStringExtra(EXTRA_URL)
        }

        if (entry != null) {
            bookmarkPostViewModel.initialize(this, entry, editData)
        }
        else if (url != null && URLUtil.isNetworkUrl(url)) {
            bookmarkPostViewModel.initialize(this, url, editData)
        }
        else {
            showToast(R.string.msg_post_bookmark_failed)
            Log.e("BookmarkPostActivity", "entry or url is not set")
            finish()
        }
    }

    // ------ //

    /** キャンセル時には編集情報を返す */
    private fun setCancelResult() {
        val intent = Intent().also {
            it.putObjectExtra(RESULT_ENTRY, bookmarkPostViewModel.entry.value)
            it.putObjectExtra(RESULT_EDIT_DATA, bookmarkPostViewModel.editData)
        }
        setResult(RESULT_CANCELED, intent)
    }

    /** 投稿成功時にはブクマ結果を返す */
    private fun setSuccessResult(bookmarkResult: BookmarkResult) {
        val intent = Intent().also {
            it.putObjectExtra(RESULT_ENTRY, bookmarkPostViewModel.entry.value)
            it.putObjectExtra(RESULT_BOOKMARK, bookmarkResult)
        }
        setResult(RESULT_OK, intent)
    }

    // ------ //

    /** 戻るボタンで閉じる場合、編集中のコメントを保存しておく */
    override fun onBackPressed() {
        hideSoftInputMethod()
        setCancelResult()
        finish()
    }

    /** Activityの外側をタップして閉じる際に、結果を渡しておく */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isOutOfBounds(event)) {
            if (!bookmarkPostViewModel.closeOnTouchOutside) return false
            if (event?.action == MotionEvent.ACTION_DOWN) {
                hideSoftInputMethod()
                setCancelResult()
            }
        }
        return super.onTouchEvent(event)
    }

    /** Activityの外側をタップしたかを判別する */
    private fun isOutOfBounds(event: MotionEvent?): Boolean {
        if (event == null) return false
        val x = event.x.toInt()
        val y = event.y.toInt()
        val dialogBounds = Rect()
        window.decorView.getHitRect(dialogBounds)
        return !dialogBounds.contains(x, y)
    }
}
