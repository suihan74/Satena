package com.suihan74.satena.scenes.browser.bookmarks

import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface IgnoredEntriesRepositoryInterface {
    /** アプリ側で設定した非表示ワード */
    val ignoredWords : List<String>

    /** ブクマ画面用のNGワード設定を取得する */
    suspend fun loadIgnoredWordsForBookmarks()
}

// ------ //

class IgnoredEntriesRepository(
    private val dao: IgnoredEntryDao
) : IgnoredEntriesRepositoryInterface {
    /** アプリ側で設定した非表示ワード */
    private var _ignoredWords : List<String> = emptyList()

    override val ignoredWords: List<String>
        get() = _ignoredWords

    /** NGワードを取得する */
    override suspend fun loadIgnoredWordsForBookmarks() = withContext(Dispatchers.IO) {
        _ignoredWords = dao.getEntriesForBookmarks()
            .map { it.query }
    }
}
