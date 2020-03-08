package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import kotlinx.coroutines.launch

/**
 * 現在値と同じ値のセット時に通知を発生しないMutableLiveData
 */
class SingleUpdateMutableLiveData<T>(initialValue: T? = null) : MutableLiveData<T>(initialValue) {
    override fun setValue(value: T?) {
        if (value != this.value) {
            super.setValue(value)
        }
    }

    override fun postValue(value: T?) {
        if (value != this.value) {
            super.postValue(value)
        }
    }
}

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

    fun initialize(onError: ((Throwable)->Unit)? = null) = viewModelScope.launch {
        repository.initialize(onError)
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

    class Factory(private val repository: EntriesRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            EntriesViewModel(repository) as T
    }
}
