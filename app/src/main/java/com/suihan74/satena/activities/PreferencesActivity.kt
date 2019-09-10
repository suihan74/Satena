package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.fragments.PreferencesFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesActivity : ActivityBase() {
    override val containerId = R.id.preferences_layout
    override val progressBarId: Int? = R.id.detail_progress_bar
    override val progressBackgroundId: Int? = R.id.click_guard

    private lateinit var mPrefsFragment : PreferencesFragment
    private var themeChanged : Boolean = false

    companion object {
        const val EXTRA_THEME_CHANGED = "com.suihan74.statena.activities.PreferencesActivity.theme_changed"
        const val EXTRA_CURRENT_TAB = "com.suihan74.statena.activities.PreferencesActivity.current_tab"
        const val EXTRA_RELOAD_ALL_PREFERENCES = "com.suihan74.statena.activities.PreferencesActivity.reload_all_preferences"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        if (isThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }
        else {
            setTheme(R.style.AppTheme_Light)
        }
        setContentView(R.layout.activity_preferences)

        themeChanged = intent.getBooleanExtra(EXTRA_THEME_CHANGED, false)

        val invokeReload = intent.getBooleanExtra(EXTRA_RELOAD_ALL_PREFERENCES, false)
        if (invokeReload) {
            reloadAllPreferences()
        }

        mPrefsFragment = PreferencesFragment.createInstance(themeChanged)
        showFragment(mPrefsFragment)
    }

    fun onClickedTab(view: View) = mPrefsFragment.onClickedTab(view)

    /** ファイルから設定を読み込んだ場合，このメソッドを使用して変更内容を適用する */
    fun reloadAllPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val isNoticeServiceEnabled = prefs.getBoolean(PreferenceKey.BACKGROUND_CHECKING_NOTICES)
        if (isNoticeServiceEnabled) {
            SatenaApplication.instance.startNotificationService()
        }
        else {
            SatenaApplication.instance.stopNotificationService()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed {
            if (themeChanged) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                true
            }
            else {
                false
            }
        }
    }
}
