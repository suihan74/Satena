package com.suihan74.satena.scenes.webview

import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences

class BrowserRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>
) {
    /** テーマ */
    val themeId by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

    /** サインイン状態 */
    val signedIn: Boolean
        get() = client.signedIn()

    /** サインインを行う */
    suspend fun initialize() {
        accountLoader.signInAccounts(reSignIn = false)
    }

    /** BookmarksEntryを取得 */
    suspend fun getBookmarksEntry(url: String) : BookmarksEntry {
        return client.getBookmarksEntryAsync(url).await()
    }
}
