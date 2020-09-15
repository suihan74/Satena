package com.suihan74.satena.scenes.webview

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.utilities.SingleUpdateMutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserViewModel(
    val repository: BrowserRepository,
    initialUrl: String
) : ViewModel() {
    /** TODO: アドレスバー検索で使用する検索エンジン(仮置き) */
    val searchEngine : String =
        "https://www.google.com/search?q="

    /** TODO: ユーザーエージェント(仮置き) */
    val userAgent : SingleUpdateMutableLiveData<String?> by lazy {
        SingleUpdateMutableLiveData(
            null//"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36"
        )
    }

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

    /** JavaScriptを有効にする */
    val javascriptEnabled by lazy {
        SingleUpdateMutableLiveData(true)
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
