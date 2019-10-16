package com.suihan74.satena.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.adapters.CategoriesAdapter
import com.suihan74.satena.fragments.*
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ActivityBase() {
    private var entriesShowed = true

    override val containerId = R.id.main_layout
    override val progressBarId = R.id.main_progress_bar
    override val progressBackgroundId = R.id.click_guard

    private lateinit var mDrawerToggle : ActionBarDrawerToggle
    private lateinit var mDrawer : DrawerLayout
    private lateinit var mCategoriesAdapter: CategoriesAdapter

    private var mIsFABMenuOpened = false
    private var mIsFABMenuBackgroundActive = false
    private var mUsingTerminationDialog = true

    private lateinit var mHomeCategory : Category
    private var mCurrentCategory : Category? = null

    val homeCategory
        get() = mHomeCategory

    var currentCategory : Category
        get() = if (mCurrentCategory == null) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
            mCurrentCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
            mCurrentCategory!!
        }
        else {
            mCurrentCategory!!
        }
        set(value) {
            mCurrentCategory = value
        }

    companion object {
        private const val EXTRA_BASE = "com.suihan74.satena.activities.MainActivity."
        const val EXTRA_DISPLAY_USER = EXTRA_BASE + "displayUser"
        const val EXTRA_DISPLAY_TAG = EXTRA_BASE + "displayTag"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean("entries_showed", entriesShowed)
            putInt("mCurrentCategory", currentCategory.int)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)

        // テーマの設定
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        if (isThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }
        else {
            setTheme(R.style.AppTheme_Light)
        }
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.hide()

        if ((SatenaApplication.instance.isFirstLaunch && !HatenaClient.signedIn()) || !entriesShowed) {
            // 初回起動時にはログイン画面に遷移
            entriesShowed = false

            val intent = Intent(this, HatenaAuthenticationActivity::class.java)
            startActivity(intent)

            SatenaApplication.instance.isFirstLaunch = false
        }

        // DrawerLayoutの設定
        mDrawer = findViewById(R.id.drawer_layout)
        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawer, toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                actionBar?.title = title
                invalidateOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                actionBar?.title = title
                invalidateOptionsMenu()
            }
        }
        mDrawer.addDrawerListener(mDrawerToggle)
        mDrawerToggle.isDrawerIndicatorEnabled = false
        mDrawerToggle.syncState()

        // メニューボタンの設定
        val menuButton = findViewById<FloatingActionButton>(R.id.entries_menu_button).apply {
            setOnClickListener {
                if (mIsFABMenuOpened) {
                    closeFABMenu()
                } else {
                    openFABMenu()
                }
            }
        }

        findViewById<View>(R.id.entries_menu_button_guard).setOnClickListener {
            menuButton.callOnClick()
        }

        findViewById<View>(R.id.entries_menu_background_guard_full).setOnClickListener {
            closeFABMenu()
        }

        // 通知ボタン
        val noticesButton = findViewById<FloatingActionButton>(R.id.entries_menu_notices_button)
        noticesButton.setOnClickListener {
            closeFABMenu()
            showFragment(NoticesFragment.createInstance(), null)
        }

        // カテゴリボタン
        val categoryButton = findViewById<FloatingActionButton>(R.id.entries_menu_categories_button)
        categoryButton.setOnClickListener {
            mDrawer.openDrawer(GravityCompat.END)
            closeFABMenu()
        }

        // 設定ボタン
        val preferencesButton = findViewById<FloatingActionButton>(R.id.entries_menu_preferences_button)
        preferencesButton.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        // カテゴリリストの作成
        val categories = if (HatenaClient.signedIn()) {
            Category.valuesWithSignedIn()
        }
        else {
            Category.valuesWithoutSignedIn()
        }

        findViewById<RecyclerView>(R.id.categories_list).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            mCategoriesAdapter = object : CategoriesAdapter(categories) {
                override fun onItemClicked(category: Category) {
                    refreshEntriesFragment(category)
                    mDrawer.closeDrawer(GravityCompat.END)
                }
            }
            adapter = mCategoriesAdapter
        }

        if (!isFragmentShowed()) {
            entriesShowed = true

            showProgressBar()
            launch(Dispatchers.Main) {
                try {
                    AccountLoader.signInAccounts(applicationContext)
                }
                catch (e: Exception) {
                    showToast("アカウント認証失敗")
                    Log.e("FailedToAuth", Log.getStackTraceString(e))
                }
                finally {
                    val category = if (savedInstanceState == null) {
                        val home = Category.fromInt(prefs.get(PreferenceKey.ENTRIES_HOME_CATEGORY))
                        if (home.requireSignedIn && !HatenaClient.signedIn()) {
                            // ログインが必要なカテゴリがホームに設定されているのにログインしていないとき，「総合」に設定し直す
                            prefs.edit {
                                putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.All.int)
                            }
                            Category.All
                        }
                        else {
                            home
                        }
                    }
                    else {
                        entriesShowed = savedInstanceState.getBoolean("entries_showed")
                        Category.fromInt(savedInstanceState.getInt("mCurrentCategory"))
                    }
                    mCurrentCategory = category

                    setMyBookmarkButton()
                    mCategoriesAdapter.setCategories(
                        if (HatenaClient.signedIn())
                            Category.valuesWithSignedIn()
                        else
                            Category.valuesWithoutSignedIn())

                    val displayUser = intent.getStringExtra(EXTRA_DISPLAY_USER)
                    val displayTag = intent.getStringExtra(EXTRA_DISPLAY_TAG)

                    when {
                        displayUser != null -> {
                            val fragment = UserEntriesFragment.createInstance(displayUser)
                            showFragment(fragment)
                        }

                        displayTag != null -> {
                            val fragment = SearchEntriesFragment.createInstance(displayTag, SearchType.Tag)
                            showFragment(fragment)
                        }

                        else -> {
                            refreshEntriesFragment(category, true)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        closeFABMenu()
        // 設定を再読み込み
        refreshPreferences()

        setMyBookmarkButton()

        mCategoriesAdapter.setCategories(
            if (HatenaClient.signedIn())
                Category.valuesWithSignedIn()
            else
                Category.valuesWithoutSignedIn())

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        toolbar?.run {
            layoutParams = (layoutParams as AppBarLayout.LayoutParams).apply {
                val switchToolbarDisplay = prefs.getBoolean(PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING)
                scrollFlags =
                    if (switchToolbarDisplay)
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    else 0
            }
        }

        updateToolbar()

        SatenaApplication.instance.setConnectionActivatedListener {
            mCategoriesAdapter.setCategories(
                if (HatenaClient.signedIn())
                    Category.valuesWithSignedIn()
                else
                    Category.valuesWithoutSignedIn())

            setMyBookmarkButton()

            refreshEntriesFragment(currentCategory)
            hideProgressBar()
        }
    }

    private fun refreshPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        mIsFABMenuBackgroundActive = prefs.getBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD)
        mUsingTerminationDialog = prefs.getBoolean(PreferenceKey.USING_TERMINATION_DIALOG)
        mHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        // TODO: 他のログイン必要画面に対応すること
        if (mHomeCategory == Category.MyBookmarks && !HatenaClient.signedIn()) {
            mHomeCategory = Category.All
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ログイン状態によってログイン/マイブックマークボタンを切り替える
    private fun setMyBookmarkButton() {
        val myBookmarkButton = findViewById<FloatingActionButton>(R.id.entries_menu_my_bookmarks_button)
        val myBookmarkDesc = findViewById<TextView>(R.id.entries_menu_my_bookmarks_desc)

        if (HatenaClient.signedIn()) {
            myBookmarkButton.setImageResource(R.drawable.ic_mybookmarks)
            myBookmarkDesc.text = "マイブックマーク"
            myBookmarkButton.setOnClickListener {
                closeFABMenu()
                refreshEntriesFragment(Category.MyBookmarks)
            }
        }
        else {
            myBookmarkButton.setImageResource(R.drawable.ic_baseline_person_add)
            myBookmarkDesc.text = "ログイン"
            myBookmarkButton.setOnClickListener {
                closeFABMenu()
                val intent = Intent(this, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun openFABMenuAnimation(layoutId: Int, descId: Int, dimenId: Int) {
        val layout = findViewById<View>(layoutId)
        val desc = findViewById<View>(descId)

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

    private fun closeFABMenuAnimation(layoutId: Int, descId: Int) {
        val layout = findViewById<View>(layoutId)
        val desc = findViewById<View>(descId)

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


    private fun openFABMenu() {
        if (mIsFABMenuOpened) return

        mIsFABMenuOpened = true

        if (mIsFABMenuBackgroundActive) {
            val menuBackground = findViewById<View>(R.id.entries_menu_background_guard_full)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            menuBackground.visibility = View.VISIBLE
        }

        val clickGuard = findViewById<View>(R.id.entries_menu_background_guard)
        clickGuard.visibility = View.VISIBLE

        if (HatenaClient.signedIn() && currentFragment !is NoticesFragment) {
            openFABMenuAnimation(
                R.id.entries_menu_notices_layout,
                R.id.entries_menu_notices_desc,
                R.dimen.dp_238
            )
        }
        openFABMenuAnimation(
            R.id.entries_menu_categories_layout,
            R.id.entries_menu_categories_desc,
            R.dimen.dp_180
        )
        openFABMenuAnimation(
            R.id.entries_menu_my_bookmarks_layout,
            R.id.entries_menu_my_bookmarks_desc,
            R.dimen.dp_122
        )
        openFABMenuAnimation(
            R.id.entries_menu_settings_layout,
            R.id.entries_menu_preferences_desc,
            R.dimen.dp_64
        )

        val menuButton = findViewById<FloatingActionButton>(R.id.entries_menu_button)
        menuButton.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeFABMenu() {
        if (!mIsFABMenuOpened) return

        mIsFABMenuOpened = false
        findViewById<View>(R.id.entries_menu_background_guard_full).visibility = View.GONE
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        val clickGuard = findViewById<View>(R.id.entries_menu_background_guard)
        clickGuard.visibility = View.GONE

        closeFABMenuAnimation(
            R.id.entries_menu_notices_layout,
            R.id.entries_menu_notices_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_categories_layout,
            R.id.entries_menu_categories_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_my_bookmarks_layout,
            R.id.entries_menu_my_bookmarks_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_settings_layout,
            R.id.entries_menu_preferences_desc
        )

        val menuButton = findViewById<FloatingActionButton>(R.id.entries_menu_button)
        menuButton.setImageResource(R.drawable.ic_baseline_menu_white)
    }

    private fun refreshEntriesFragment(category: Category, forceUpdate: Boolean = false) {
        if (mCurrentCategory == category && !forceUpdate) return

        val fragment = when (category) {
            Category.MyTags -> {
                val fragment = UserTagsEntriesFragment.createInstance(
                    HatenaClient.account!!.name
                )
                replaceFragment(fragment)
            }

            Category.Search -> {
                val fragment = SearchEntriesFragment.createInstance()
                replaceFragment(fragment)
            }

            Category.MyHotEntries -> {
                val fragment = MyHotEntriesFragment.createInstance()
                replaceFragment(fragment)
            }

            Category.MyStars -> {
                val fragment = MyStarsFragment.createInstance()
                replaceFragment(fragment)
            }

            Category.StarsReport -> {
                val fragment = StarsReportFragment.createInstance()
                replaceFragment(fragment)
            }

            Category.Maintenance -> {
                val fragment = MaintenanceInformationFragment.createInstance()
                replaceFragment(fragment)
            }

            else -> {
                when (val fragment = currentFragment) {
                    is EntriesFragment -> {
                        fragment.apply {
                            refreshEntriesTabs(category)
                        }
                    }

                    else -> {
                        val entriesFragment = EntriesFragment.createInstance(category)
                        replaceFragment(entriesFragment)
                    }
                }
            }
        }

        updateToolbar(fragment)
        currentCategory = category
    }

    override fun onBackPressed() {
        // 戻るボタンでメニューを閉じる
        when {
            mIsFABMenuOpened ->
                closeFABMenu()

            mDrawer.isDrawerOpen(GravityCompat.END) ->
                mDrawer.closeDrawer(GravityCompat.END)

            currentFragment is NoticesFragment ->
                popFragment()

            mCurrentCategory != mHomeCategory -> // ホームカテゴリ以外のカテゴリにいる場合はホームに戻る
                refreshEntriesFragment(mHomeCategory)

            mUsingTerminationDialog ->
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle("確認")
                .setMessage("アプリを終了しますか？")
                .setIcon(R.drawable.ic_baseline_help)
                .setPositiveButton("OK") { _, _ -> finishAndRemoveTask() }
                .setNegativeButton("Cancel", null)
                .create()
                .show()

            else ->
                super.onBackPressed()
        }
    }
}
