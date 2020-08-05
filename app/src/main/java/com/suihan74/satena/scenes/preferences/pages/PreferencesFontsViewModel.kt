package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.FontSettings
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesFontsViewModel(
    prefs: SafeSharedPreferences<PreferenceKey>
) : PreferencesViewModel(prefs) {

    /** エントリタイトル */
    val entryTitle = createLiveData<FontSettings>(
        PreferenceKey.FONT_ENTRY_TITLE
    )

    /** ブコメユーザー名 */
    val bookmarkUser = createLiveData<FontSettings>(
        PreferenceKey.FONT_BOOKMARK_USER
    )

    /** ブコメ本文 */
    val bookmarkComment = createLiveData<FontSettings>(
        PreferenceKey.FONT_BOOKMARK_COMMENT
    )

    class Factory(
        private val prefs: SafeSharedPreferences<PreferenceKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            PreferencesFontsViewModel(prefs) as T
    }
}
