package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_entries2.*

class EntriesActivity : AppCompatActivity() {
    companion object {
        /** アクティビティ生成時にCategory.Siteに遷移、表示するURL */
        const val EXTRA_SITE_URL = "EntriesActivity.EXTRA_SITE_URL"

        /** アクティビティ生成時にCategory.Userに遷移、表示するユーザー */
        const val EXTRA_USER = "EntriesActivity.EXTRA_USER"

        /** アクティビティ生成時にCategory.Searchに遷移、タグ検索を行う */
        const val EXTRA_SEARCH_TAG = "EntriesActivity.EXTRA_SEARCH_TAG"
    }

    private lateinit var viewModel : EntriesViewModel

    /** ドロワーの開閉状態 */
    private val isDrawerOpened : Boolean
        get() = drawer_layout.isDrawerOpen(categories_list)

    /** FABメニューの開閉状態 */
    private var isFABMenuOpened : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(
            if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
            else R.style.AppTheme_Light
        )

        val factory = EntriesViewModel.Factory(
            EntriesRepository(
                client = HatenaClient,
                accountLoader = AccountLoader(
                    this,
                    HatenaClient,
                    MastodonClientHolder
                ),
                prefs = prefs,
                noticesPrefs = SafeSharedPreferences.create(this),
                historyPrefs = SafeSharedPreferences.create(this),
                ignoredEntryDao = SatenaApplication.instance.ignoredEntryDao
            )
        )
        viewModel = ViewModelProvider(this, factory)[EntriesViewModel::class.java]
        viewModel.initialize(
            onSuccess = {
                if (savedInstanceState == null) {
                    showContents()
                }
            },
            onError = { e ->
                showToast(R.string.msg_auth_failed)
                Log.e("error", Log.getStackTraceString(e))
            }
        )

        // データバインディング
        DataBindingUtil.setContentView<ActivityEntries2Binding>(
            this,
            R.layout.activity_entries2
        ).apply {
            lifecycleOwner = this@EntriesActivity
            vm = viewModel
        }

        // カテゴリリスト初期化
        categories_list.adapter = CategoriesAdapter().apply {
            setOnItemClickedListener { category ->
                drawer_layout.closeDrawers()
                showCategory(category)
            }
        }

        // カテゴリリストのドロワ表示状態切り替えを監視する
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                // IMEを明示的に閉じないと被ってしまう
                hideSoftInputMethod()
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        // --- Event listeners ---

        // FABメニュー表示ボタン
        entries_menu_button.setOnClickListener {
            if (isFABMenuOpened) {
                closeFABMenu()
            }
            else {
                openFABMenu()
            }
        }

        // 通知リスト画面表示ボタン
        entries_menu_notices_button.setOnClickListener {
            closeFABMenu()
            showCategory(Category.Notices)
        }

        // カテゴリリスト表示ボタン
        entries_menu_categories_button.setOnClickListener {
            closeFABMenu()
            drawer_layout.openDrawer(categories_list)
        }

        // サインイン/マイブックマークボタン
        entries_menu_my_bookmarks_button.setOnClickListener {
            closeFABMenu()
            if (viewModel.signedIn.value == true) {
                // マイブックマークを表示
                showCategory(Category.MyBookmarks)
            }
            else {
                // サインイン画面に遷移
                val intent = Intent(this, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }
        }

        // 設定画面表示ボタン
        entries_menu_preferences_button.setOnClickListener {
            closeFABMenu()
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        // メニューを表示している間の黒背景
        entries_menu_background_guard_full.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    closeFABMenu()
                    true
                }

                else -> false
            }
        }

        // --- Observers ---

        viewModel.signedIn.observe(this, Observer {
            if (it) {
                entries_menu_notices_button.show()
            }
            else {
                entries_menu_notices_button.hide()
            }
            entries_menu_notices_desc.visibility = it.toVisibility()
        })

        // 通信状態の変更を監視
        var isNetworkReceiverInitialized = false
        SatenaApplication.instance.networkReceiver.state.observe(this, Observer { state ->
            if (!isNetworkReceiverInitialized) {
                isNetworkReceiverInitialized = true
                return@Observer
            }

            if (state == NetworkReceiver.State.CONNECTED) {
                viewModel.initialize()
            }
        })
    }

    /** 最初に表示するコンテンツの用意 */
    private fun showContents() {
        val user = intent.getStringExtra(EXTRA_USER)
        val siteUrl = intent.getStringExtra(EXTRA_SITE_URL)
        val searchTag = intent.getStringExtra(EXTRA_SEARCH_TAG)

        when {
            user != null -> showUserEntries(user)

            siteUrl != null -> showSiteEntries(siteUrl)

            searchTag != null -> showSearchEntries(searchTag, SearchType.Tag)

            else -> showCategory(viewModel.homeCategory)
        }
    }

    override fun onResume() {
        super.onResume()

        // ツールバーを隠す設定を反映
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                if (viewModel.hideToolbarByScroll) AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                else 0
        }

        // アクションバー設定
        setSupportActionBar(toolbar)
    }

    /** 戻るボタンの挙動 */
    override fun onBackPressed() {
        when {
            isDrawerOpened -> drawer_layout.closeDrawer(categories_list)

            isFABMenuOpened -> closeFABMenu()

            else -> {
                super.onBackPressed()
                if (supportFragmentManager.backStackEntryCount == 0) {
                    finish()
                }
            }
        }
    }

    /** (カテゴリメニューから遷移できる)カテゴリを選択 */
    private fun showCategory(category: Category) {
        showContentFragment(category) {
            category.createFragment()
        }
    }

    /** Category.Siteに遷移 */
    fun showSiteEntries(siteUrl: String) {
        showContentFragment(Category.Site) {
            Category.Site.createSiteFragment(siteUrl)
        }
    }

    /** Category.Userに遷移 */
    fun showUserEntries(user: String) {
        showContentFragment(Category.User) {
            Category.User.createUserFragment(user)
        }
    }

    /** Category.Search */
    fun showSearchEntries(query: String, searchType: SearchType) {
        showContentFragment(Category.Search) {
            Category.Search.createSearchFragment(query, searchType)
        }
    }

    /** EntriesFragment遷移処理 */
    private fun showContentFragment(category: Category, fragmentGenerator: ()->EntriesFragment) {
        // 現在トップにある画面と同じカテゴリには連続して遷移しない
        if (supportFragmentManager.topBackStackEntry?.name == category.name) return

        // 既に一度表示されているカテゴリの場合再利用する
        val existed = supportFragmentManager.findFragmentByTag(category.name)
        val fragment = existed ?: fragmentGenerator()

        supportFragmentManager.beginTransaction().run {
            replace(R.id.main_layout, fragment, category.name)
            addToBackStack(category.name)
            commit()
        }
    }

    /** AppBarを強制的に表示する */
    fun showAppBar() {
        appbar_layout.setExpanded(true, true)
    }

    // --- FAB表示アニメーション ---

    /** FABメニュー各項目のオープン時移動アニメーション */
    private fun openFABMenuAnimation(layout: View, desc: View, dimenId: Int) {
        layout.visibility = View.VISIBLE
        layout.animate()
            .withEndAction {
                desc.animate()
                    .translationXBy(100f)
                    .translationX(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationY(-resources.getDimension(dimenId))
            .alphaBy(0.0f)
            .alpha(1.0f)
            .duration = 100
    }

    /** FABメニュー各項目のクローズ時移動アニメーション */
    private fun closeFABMenuAnimation(layout: View, desc: View) {
        if (layout.visibility != View.VISIBLE) return

        desc.animate()
            .withEndAction {
                layout.animate()
                    .withEndAction {
                        layout.visibility = View.INVISIBLE
                    }
                    .translationY(0f)
                    .alphaBy(1.0f)
                    .alpha(0.0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }


    /** FABメニューを開く */
    private fun openFABMenu() {
        if (isFABMenuOpened) return

        // メニュー表示中はIMEを隠す
        hideSoftInputMethod()

        isFABMenuOpened = true

        if (viewModel.isFABMenuBackgroundActive) {
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            entries_menu_background_guard_full.visibility = View.VISIBLE
        }

        entries_menu_background_guard.visibility = View.VISIBLE

        openFABMenuAnimation(
            entries_menu_notices_layout,
            entries_menu_notices_desc,
            R.dimen.dp_238
        )
        openFABMenuAnimation(
            entries_menu_categories_layout,
            entries_menu_categories_desc,
            R.dimen.dp_180
        )
        openFABMenuAnimation(
            entries_menu_my_bookmarks_layout,
            entries_menu_my_bookmarks_desc,
            R.dimen.dp_122
        )
        openFABMenuAnimation(
            entries_menu_settings_layout,
            entries_menu_preferences_desc,
            R.dimen.dp_64
        )

        entries_menu_button.setImageResource(R.drawable.ic_baseline_close)
    }

    /** FABメニューを閉じる */
    private fun closeFABMenu() {
        if (!isFABMenuOpened) return

        isFABMenuOpened = false
        entries_menu_background_guard_full.visibility = View.GONE
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        entries_menu_background_guard.visibility = View.GONE

        closeFABMenuAnimation(
            entries_menu_notices_layout,
            entries_menu_notices_desc
        )
        closeFABMenuAnimation(
            entries_menu_categories_layout,
            entries_menu_categories_desc
        )
        closeFABMenuAnimation(
            entries_menu_my_bookmarks_layout,
            entries_menu_my_bookmarks_desc
        )
        closeFABMenuAnimation(
            entries_menu_settings_layout,
            entries_menu_preferences_desc
        )

        entries_menu_button.setImageResource(R.drawable.ic_baseline_menu_white)
    }
}
