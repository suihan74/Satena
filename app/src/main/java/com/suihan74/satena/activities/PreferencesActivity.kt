package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.suihan74.utilities.FragmentContainerActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.fragments.PreferencesFragment
import com.suihan74.satena.R

class PreferencesActivity : ActivityBase() {
    override val containerId = R.id.preferences_layout
    override fun getProgressBarId(): Int? = R.id.progress_bar
    override fun getProgressBackgroundId(): Int? = R.id.click_guard

    private lateinit var mPrefsFragment : PreferencesFragment
    private var themeChanged : Boolean = false

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

        themeChanged = intent.getBooleanExtra("theme_changed", false)

        mPrefsFragment = PreferencesFragment.createInstance(themeChanged)
        showFragment(mPrefsFragment)
    }

    fun onClickedTab(view: View) = mPrefsFragment.onClickedTab(view)

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
