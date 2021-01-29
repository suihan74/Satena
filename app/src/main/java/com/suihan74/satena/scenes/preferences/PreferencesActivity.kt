package com.suihan74.satena.scenes.preferences

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getObjectExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesActivity : ActivityBase() {
    class ViewModel : androidx.lifecycle.ViewModel() {
        val currentTab : MutableLiveData<PreferencesTabMode> by lazy {
            MutableLiveData(PreferencesTabMode.INFORMATION)
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ViewModel::class.java]
    }

    override val progressBarId: Int? = R.id.detail_progress_bar
    override val progressBackgroundId: Int? = R.id.click_guard
    override val toolbar : Toolbar
        get() = findViewById(R.id.preferences_toolbar)

    private lateinit var mViewPager : ViewPager
    private lateinit var mTabAdapter : PreferencesTabAdapter

    private var themeChanged : Boolean = false

    companion object {
        const val EXTRA_THEME_CHANGED = "theme_changed"
        const val EXTRA_CURRENT_TAB = "current_tab"
        const val EXTRA_RELOAD_ALL_PREFERENCES = "reload_all_preferences"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(Theme.themeId(prefs))
        setContentView(R.layout.activity_preferences)
        setSupportActionBar(toolbar)

        themeChanged = intent.getBooleanExtra(EXTRA_THEME_CHANGED, false)

        // 初期タブが指定されている場合
        intent.getObjectExtra<PreferencesTabMode>(EXTRA_CURRENT_TAB)?.let {
            viewModel.currentTab.value = it
        }

        val invokeReload = intent.getBooleanExtra(EXTRA_RELOAD_ALL_PREFERENCES, false)
        if (invokeReload) {
            showProgressBar()
            launch(Dispatchers.Main) {
                reloadAllPreferences()
                initializeContents()
                hideProgressBar()
            }
        }
        else {
            initializeContents()
        }
    }

    private fun initializeContents() {
        mTabAdapter = PreferencesTabAdapter(supportFragmentManager)
        mViewPager = findViewById<ViewPager>(R.id.preferences_view_pager).also { pager ->
            // 環状スクロールできるように細工
            pager.adapter = mTabAdapter
            pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                private var jumpPosition = -1

                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager.SCROLL_STATE_IDLE && jumpPosition > 0) {
                        mViewPager.setCurrentItem(jumpPosition, false)
                        jumpPosition = -1
                    }
                }

                // position & jumpPosition => 1 ~ actualCount  // head&tailを含むインデックス
                // fixedPosition => 0 ~ actualCount-1  // head&tailを無視したコンテンツのインデックス(0: ACCOUNTS, 1: GENERALS, ...)
                override fun onPageSelected(position: Int) {
                    for (i in 0 until mTabAdapter.getActualCount()) {
                        val btn = findViewById<ImageButton>(mTabAdapter.getIconId(i))
                        btn?.setBackgroundColor(Color.TRANSPARENT)
                    }

                    jumpPosition = when (position) {
                        PreferencesTabMode.DUMMY_HEAD.int -> mTabAdapter.getActualCount()
                        PreferencesTabMode.DUMMY_TAIL.int -> 1
                        else -> position
                    }

                    val prevTabId = viewModel.currentTab.value?.int
                    if (prevTabId != null) {
                        mTabAdapter.findFragment(pager, prevTabId).alsoAs<TabItem> { fragment ->
                            fragment.onTabUnselected()
                        }
                    }

                    val tab = PreferencesTabMode.fromId(jumpPosition)
                    viewModel.currentTab.value = tab

                    mTabAdapter.findFragment(pager, tab.int).alsoAs<TabItem> { fragment ->
                        fragment.onTabSelected()
                    }

                    val btn = findViewById<ImageButton>(mTabAdapter.getIconId(tab.int - 1))
                    btn?.setBackgroundColor(ContextCompat.getColor(this@PreferencesActivity, R.color.colorPrimary))
                    title = "設定 > ${getString(tab.titleId)}"
                    invalidateOptionsMenu()
                }
            })
        }

        val tab = viewModel.currentTab.value!!

        val position = tab.int
        mViewPager.setCurrentItem(position, false)
        title = "設定 > ${getString(tab.titleId)}"
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item!!)
    }

    fun onClickedTab(view: View) {
        mViewPager.currentItem = mTabAdapter.getIndexFromIconId(view.id)
    }

    override fun onRequestPermissionsResult(pairs: List<Pair<String, Int>>) {
        val currentFragment = mTabAdapter.findFragment(mViewPager, mViewPager.currentItem)
        if (currentFragment is PermissionRequestable) {
            currentFragment.onRequestPermissionsResult(pairs)
        }
    }

    /** ファイルから設定を読み込んだ場合，このメソッドを使用して変更内容を適用する */
    private suspend fun reloadAllPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        try {
            // 再ログイン
            AccountLoader(
                applicationContext,
                HatenaClient,
                MastodonClientHolder
            ).signInAccounts(true)
        }
        catch (e: Throwable) {
            Log.e("FailedToReload", e.message ?: "")
        }

        // 通知サービス開始
        val isNoticeServiceEnabled = prefs.getBoolean(PreferenceKey.BACKGROUND_CHECKING_NOTICES)
        if (isNoticeServiceEnabled) {
            SatenaApplication.instance.startCheckingNotificationsWorker(this)
        }
        else {
            SatenaApplication.instance.stopCheckingNotificationsWorker(this)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed {
            if (themeChanged) {
                val intent = Intent(this, EntriesActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                true
            }
            else false
        }
    }
}
