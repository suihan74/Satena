package com.suihan74.satena.scenes.browser

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SingleUpdateMutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserViewModel(
    val repository: BrowserRepository,
    initialUrl: String
) : ViewModel() {
    /** テーマ */
    val themeId : Int
        get() = repository.themeId

    /** 表示中のページURL */
    val url by lazy {
        SingleUpdateMutableLiveData(initialUrl).apply {
            observeForever {
                addressText.value = it
                loadBookmarksEntry(it)
            }
        }
    }

    /** 表示中のページタイトル */
    val title by lazy {
        MutableLiveData("")
    }

    /** アドレスバー検索で使用する検索エンジン(仮置き) */
    val searchEngine : String
        get() = repository.searchEngine.value!!

    /** ユーザーエージェント(仮置き) */
    val userAgent by lazy {
        repository.userAgent
    }

    /** JavaScriptを有効にする */
    val javascriptEnabled by lazy {
        repository.javascriptEnabled
    }

    /** URLブロッキングを使用する */
    val useUrlBlocking by lazy {
        repository.useUrlBlocking
    }

    /** アドレスバーの入力内容 */
    val addressText by lazy {
        SingleUpdateMutableLiveData("")
    }

    // ------ //

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry?>(null)
    }

    // ------ //

    // ページ読み込み完了時に呼ぶ処理
    var onPageFinished: Listener<String>? = null

    fun setOnPageFinishedListener(listener: Listener<String>?) {
        onPageFinished = listener
    }

    // ------ //

    /** BookmarksEntryを更新 */
    private fun loadBookmarksEntry(url: String) = viewModelScope.launch(Dispatchers.Default) {
        try {
            bookmarksEntry.postValue(repository.getBookmarksEntry(url))
        }
        catch (e: Throwable) {
            Log.e("loadBookmarksEntry", Log.getStackTraceString(e))
            bookmarksEntry.postValue(null)
        }
    }

    /** アドレスバーの入力内容に沿ってページ遷移 */
    fun goAddress() : Boolean {
        val addr = addressText.value
        return when {
            addr.isNullOrBlank() -> false

            // アドレスが渡されたら直接遷移する
            URLUtil.isValidUrl(addr) -> {
                url.value = addr
                true
            }

            // それ以外は入力内容をキーワードとして検索を行う
            else -> {
                url.value = searchEngine + Uri.encode(addr)
                true
            }
        }
    }
}
