package com.suihan74.satena.scenes.entries2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.models.Category

class EntriesViewModel(
    private val repository : EntriesRepository
) : ViewModel() {
    /** カテゴリリスト */
    val categories by lazy {
        repository.categoriesLiveData
    }

    /** 現在表示中のカテゴリ */
    val currentCategory by lazy {
        MutableLiveData<Category>(repository.homeCategory)
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

    /** FABメニューにタップ防止背景を表示する */
    val isFABMenuBackgroundActive : Boolean
        get() = repository.isFABMenuBackgroundActive

    /** アプリを終了する前にホームカテゴリに戻る */
    fun onBackPressed() : Boolean =
        if (currentCategory.value == repository.homeCategory) false
        else {
            currentCategory.value = repository.homeCategory
            true
        }


    class Factory(private val repository: EntriesRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            EntriesViewModel(repository) as T
    }
}
