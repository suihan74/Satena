package com.suihan74.satena.scenes.browser

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserViewModel(
    val repository: BrowserRepository,
    initialUrl: String
) : ViewModel() {
    private val DIALOG_BLOCK_URL by lazy { "DIALOG_BLOCK_URL" }

    /** テーマ */
    val themeId : Int
        get() = repository.themeId

    /** 表示中のページURL */
    val url by lazy {
        SingleUpdateMutableLiveData(initialUrl).apply {
            observeForever {
                addressText.value = Uri.decode(it)
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

    /** 現在表示中のページで読み込んだすべてのURL */
    val resourceUrls : List<ResourceUrl>
        get() = repository.resourceUrls

    // ------ //

    /** 表示中のページのはてなエントリURL */
    val entryUrl by lazy {
        MutableLiveData<String>("")
    }

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry?>(null)
    }

    // ------ //

    // ページ読み込み完了時に呼ぶ処理
    private var onPageFinishedListener: Listener<String>? = null

    fun setOnPageFinishedListener(listener: Listener<String>?) {
        onPageFinishedListener = listener
    }

    // ------ //

    /** BookmarksEntryを更新 */
    private fun loadBookmarksEntry(url: String) = viewModelScope.launch(Dispatchers.Default) {
        // 渡されたページURLをエントリURLに修正する
        val modifyResult = kotlin.runCatching {
            modifySpecificUrls(url)
        }
        val modifiedUrl = modifyResult.getOrNull() ?: url
        entryUrl.postValue(modifiedUrl)

        try {
            bookmarksEntry.postValue(repository.getBookmarksEntry(modifiedUrl))
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

    /** ページ読み込み開始時の処理 */
    fun onPageStarted(url: String) {
        this.title.value = url
        this.url.value = url
        repository.resourceUrls.clear()
    }

    /** ページ読み込み完了時の処理 */
    fun onPageFinished(view: WebView?, url: String) {
        this.title.value = view?.title ?: url
        repository.resourceUrls.addUnique(ResourceUrl(url, false))

        onPageFinishedListener?.invoke(url)
    }

    /** リソースを追加 */
    fun addResource(url: String, blocked: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.resourceUrls.addUnique(ResourceUrl(url, blocked))
    }

    // ------ //

    /** 新しいブロック設定を追加するダイアログを開く */
    fun openBlockUrlDialog(
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        UrlBlockingDialog.createInstance(resourceUrls).run {
            showAllowingStateLoss(fragmentManager, DIALOG_BLOCK_URL)

            setOnCompleteListener { setting ->
                val blockList = repository.blockUrls.value ?: emptyList()

                if (blockList.none { it.pattern == setting.pattern }) {
                    repository.blockUrls.value = blockList.plus(setting)
                }

                SatenaApplication.instance.showToast(R.string.msg_add_url_blocking_succeeded)
            }
        }
    }
}
