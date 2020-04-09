package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.TapEntryAction
import kotlinx.coroutines.launch

class EntriesViewModel(
    val repository : EntriesRepository
) : ViewModel() {
    /** カテゴリリスト */
    val categories by lazy {
        repository.categoriesLiveData
    }

    /** サインイン状態 */
    val signedIn by lazy {
        repository.signedInLiveData
    }

    /** サインイン/マイブックマークボタンの説明テキスト */
    val myBookmarkButtonTextId by lazy {
        MutableLiveData<Int>().also { resId ->
            signedIn.observeForever {
                resId.value =
                    if (it) R.string.my_bookmarks_button_desc_my_bookmarks
                    else R.string.my_bookmarks_button_desc_sign_in
            }
        }
    }

    /** サインイン/マイブックマークボタンのアイコン */
    val myBookmarkButtonIconId by lazy {
        MutableLiveData<Int>().also { resId ->
            signedIn.observeForever {
                 resId.value =
                     if (it) R.drawable.ic_mybookmarks
                     else R.drawable.ic_baseline_person_add
            }
        }
    }

    /** 初期化処理 */
    fun initialize(
        forceUpdate: Boolean,
        onSuccess: (()->Unit)? = null,
        onError: ((Throwable)->Unit)? = null,
        onFinally: ((Throwable?)->Unit)? = null
    ) = viewModelScope.launch {
        var error: Throwable? = null
        try {
            repository.signIn(forceUpdate)
            onSuccess?.invoke()
        }
        catch (e: Throwable) {
            error = e
            onError?.invoke(e)
        }
        finally {
            onFinally?.invoke(error)
        }
    }

    /** ホームカテゴリ */
    val homeCategory : Category
        get() = repository.homeCategory

    /** FABメニューにタップ防止背景を表示する */
    val isFABMenuBackgroundActive : Boolean
        get() = repository.isFABMenuBackgroundActive

    /** スクロールにあわせてツールバーを隠す */
    val hideToolbarByScroll : Boolean
        get() = repository.hideToolbarByScroll

    /** エントリ項目クリック時の挙動 */
    val entryClickedAction : TapEntryAction
        get() = repository.entryClickedAction

    /** エントリ項目長押し時の挙動 */
    val entryLongClickedAction : TapEntryAction
        get() = repository.entryLongClickedAction

    class Factory(private val repository: EntriesRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            EntriesViewModel(repository) as T
    }
}
