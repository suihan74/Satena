package com.suihan74.satena.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.transition.AutoTransition
import android.transition.TransitionSet
import android.util.Log
import android.view.*
import android.widget.TextView
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.activities.MainActivity
import com.suihan74.satena.activities.PreferencesActivity
import com.suihan74.satena.adapters.CategoriesAdapter
import com.suihan74.satena.adapters.tabs.EntriesTabAdapter
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class EntriesFragment : CoroutineScopeFragment(), BackPressable {
    private lateinit var mEntriesTabPager : ViewPager
    private lateinit var mDrawerToggle : ActionBarDrawerToggle
    private lateinit var mDrawer : DrawerLayout
    private lateinit var mEntriesTabLayout : TabLayout
    private lateinit var mEntriesTabAdapter : EntriesTabAdapter
    private lateinit var mCategoriesAdapter: CategoriesAdapter
    private lateinit var mView : View

    private var mIsFABMenuOpened = false
    private var mIsFABMenuBackgroundActive = false
    private lateinit var mHomeCategory : Category
    private var mCurrentCategory : Category? = null

    private var mUsingTerminationDialog = true

    private val mTasks = ArrayList<Deferred<Any>>()

    companion object {
        fun createInstance() = EntriesFragment().apply {
            enterTransition = TransitionSet().addTransition(AutoTransition())
        }

        private const val BUNDLE_BASE = "com.suihan74.satena.fragments.EntriesFragment."
        private const val BUNDLE_CATEGORY = BUNDLE_BASE + "mCategory"
        private const val BUNDLE_CURRENT_TAB = BUNDLE_BASE + "currentTab"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CATEGORY, mEntriesTabAdapter.category.int)
        outState.putInt(BUNDLE_CURRENT_TAB, mEntriesTabPager.currentItem)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定を読み込む
        refreshPreferences()

        val category = savedInstanceState?.let {
            Category.fromInt(it.getInt(BUNDLE_CATEGORY))
        } ?: mHomeCategory
        mCurrentCategory = category

        // エントリリスト用アダプタ作成
        mEntriesTabAdapter = EntriesTabAdapter(this, category)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_entries, container, false)

        val mainActivity = activity as MainActivity

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
        val textId = mainActivity.resources.getIdentifier(
            "category_${mEntriesTabAdapter.category.name.toLowerCase(Locale.ROOT)}",
            "string",
            mainActivity.packageName
        )

        val toolbar = mView.findViewById<Toolbar>(R.id.main_toolbar).apply {
            title = getString(textId)
        }
        mainActivity.setSupportActionBar(toolbar)

        // DrawerLayoutの設定
        mDrawer = mView.findViewById(R.id.entries_drawer_layout)
        mDrawerToggle = object : ActionBarDrawerToggle(mainActivity, mDrawer, toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                mainActivity.actionBar?.title = mainActivity.title
                mainActivity.invalidateOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                mainActivity.actionBar?.title = mainActivity.title
                mainActivity.invalidateOptionsMenu()
            }
        }
        mDrawer.addDrawerListener(mDrawerToggle)
        mDrawerToggle.isDrawerIndicatorEnabled = false
        mDrawerToggle.syncState()

        // メニューボタンの設定
        val menuButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_button).apply {
            setOnClickListener {
                if (mIsFABMenuOpened) {
                    closeFABMenu()
                } else {
                    openFABMenu()
                }
            }
        }

        mView.findViewById<View>(R.id.entries_menu_button_guard).setOnClickListener {
            menuButton.callOnClick()
        }

        mView.findViewById<View>(R.id.entries_menu_background_guard_full).setOnClickListener {
            closeFABMenu()
        }

        // 通知ボタン
        val noticesButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_notices_button)
        noticesButton.setOnClickListener {
            closeFABMenu()
            mainActivity.showFragment(NoticesFragment.createInstance(), null)
        }

        // カテゴリボタン
        val categoryButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_categories_button)
        categoryButton.setOnClickListener {
            mDrawer.openDrawer(Gravity.END)
            closeFABMenu()
        }

        // 設定ボタン
        val preferencesButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_preferences_button)
        preferencesButton.setOnClickListener {
            val intent = Intent(activity, PreferencesActivity::class.java)
            startActivity(intent)
        }

        // カテゴリリストの作成
        val categories = if (HatenaClient.signedIn())
            Category.valuesWithSignedIn()
        else
            Category.valuesWithoutSignedIn()

        mView.findViewById<RecyclerView>(R.id.categories_list).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            mCategoriesAdapter = object : CategoriesAdapter(categories) {
                override fun onItemClicked(category: Category) {
                    val activity = activity as FragmentContainerActivity
                    when (category) {
                        Category.MyTags -> {
                            val fragment = UserTagsEntriesFragment.createInstance(
                                HatenaClient.account!!.name
                            )
                            activity.showFragment(fragment, null)
                        }

                        Category.Search -> {
                            val fragment = SearchEntriesFragment.createInstance()
                            activity.showFragment(fragment, null)
                        }

                        Category.MyHotEntries -> {
                            val fragment = MyHotEntriesFragment.createInstance()
                            activity.showFragment(fragment, null)
                        }

                        else -> refreshEntriesTabs(category)
                    }
                    mDrawer.closeDrawer(Gravity.END)
                }
            }
            adapter = mCategoriesAdapter
        }

        // タブの設定
        mEntriesTabPager = mView.findViewById(R.id.entries_tab_pager)
        mEntriesTabPager.adapter = mEntriesTabAdapter
        mEntriesTabLayout = mView.findViewById<TabLayout>(R.id.main_tab_layout).apply {
            setupWithViewPager(mEntriesTabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(p0: TabLayout.Tab?) {}
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val fragment = mEntriesTabAdapter.findFragment(mEntriesTabPager, tab!!.position)
                    fragment.scrollToTop()
                }
            })
        }

        if (savedInstanceState == null) {
            mEntriesTabPager.currentItem = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)
        }
        else {
            val restoreTab = savedInstanceState.getInt(BUNDLE_CURRENT_TAB)
            mEntriesTabPager.currentItem = restoreTab
            launch {
                mEntriesTabAdapter.refreshAllTab(mEntriesTabPager)
            }
        }

        retainInstance = true
        return mView
    }

    override fun onBackPressed(): Boolean {
        // 戻るボタンでメニューを閉じる
        if (mIsFABMenuOpened) { closeFABMenu(); return true }
        if (mDrawer.isDrawerOpen(Gravity.END)) { mDrawer.closeDrawer(Gravity.END); return true }

        // ホームカテゴリ以外のカテゴリにいる場合はホームに戻る
        if (mEntriesTabAdapter.category != mHomeCategory) {
            for (i in 0 until mEntriesTabLayout.tabCount) {
                val tab = mEntriesTabLayout.getTabAt(i)!!
                tab.text = mEntriesTabAdapter.getPageTitle(i)
            }

            if (mTasks.isNotEmpty()) {
                for (t in mTasks) t.cancel()

                if (activity is MainActivity) {
                    val mainActivity = activity as MainActivity
                    mainActivity.hideProgressBar()
                }
            }

            refreshEntriesTabs(mHomeCategory)
            return true
        }

        return if (mUsingTerminationDialog) {
            AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setTitle("確認")
                .setMessage("アプリを終了しますか？")
                .setIcon(R.drawable.ic_baseline_help)
                .setPositiveButton("OK") { _, _ -> activity!!.finishAndRemoveTask() }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
            true
        }
        else {
            false
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

        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)
        mView.findViewById<Toolbar>(R.id.main_toolbar).run {
            layoutParams = (layoutParams as AppBarLayout.LayoutParams).apply {
                val switchToolbarDisplay = prefs.getBoolean(PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING)
                scrollFlags =
                    if (switchToolbarDisplay)
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    else 0
            }
        }

        SatenaApplication.instance.setConnectionActivatedListener {
            mCategoriesAdapter.setCategories(
                if (HatenaClient.signedIn())
                    Category.valuesWithSignedIn()
                else
                    Category.valuesWithoutSignedIn())

            val activity = activity as ActivityBase
            setMyBookmarkButton()

            refreshEntriesTabs(mEntriesTabAdapter.category)
            activity.hideProgressBar()
        }
    }

    private fun refreshPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)
        mHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        if (mHomeCategory == Category.MyBookmarks && !HatenaClient.signedIn()) {
            mHomeCategory = Category.All
        }

        mIsFABMenuBackgroundActive = prefs.getBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD)
        mUsingTerminationDialog = prefs.getBoolean(PreferenceKey.USING_TERMINATION_DIALOG)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ログイン状態によってログイン/マイブックマークボタンを切り替える
    private fun setMyBookmarkButton() {
        val activity = activity as MainActivity
        val myBookmarkButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_my_bookmarks_button)
        val myBookmarkDesc = mView.findViewById<TextView>(R.id.entries_menu_my_bookmarks_desc)

        if (HatenaClient.signedIn()) {
            myBookmarkButton.setImageResource(R.drawable.ic_mybookmarks)
            myBookmarkDesc.text = "マイブックマーク"
            myBookmarkButton.setOnClickListener {
                closeFABMenu()
                refreshEntriesTabs(Category.MyBookmarks)
            }
        }
        else {
            myBookmarkButton.setImageResource(R.drawable.ic_baseline_person_add)
            myBookmarkDesc.text = "ログイン"
            myBookmarkButton.setOnClickListener {
                closeFABMenu()
                val fragment = HatenaAuthenticationFragment.createInstance()
                activity.showFragment(fragment, null)
            }
        }
    }

    fun refreshEntriesAsync(tabPosition: Int? = null, offset: Int? = null) : Deferred<List<Entry>> =
        mEntriesTabAdapter.getEntriesAsync(tabPosition ?: mEntriesTabLayout.selectedTabPosition, offset)

    fun refreshEntriesTabs(category: Category) {
        val mainActivity = activity as MainActivity
        val toolbar = mView.findViewById<Toolbar>(R.id.main_toolbar)

        val textId = mainActivity.resources.getIdentifier("category_${category.name.toLowerCase(Locale.ROOT)}", "string", mainActivity.packageName)
        toolbar.title = getString(textId)

        mainActivity.showProgressBar()

        val categoryChanged = mCurrentCategory != category
        mCurrentCategory = category
        mEntriesTabAdapter.setCategory(mEntriesTabPager, category)

        for (tabPosition in 0 until mEntriesTabAdapter.count) {
            val tabFragment = mEntriesTabAdapter.findFragment(mEntriesTabPager, tabPosition)
            val tab = mEntriesTabLayout.getTabAt(tabPosition)!!

            mEntriesTabAdapter.setCategory(mEntriesTabPager, category)
            tab.text = mEntriesTabAdapter.getPageTitle(tabPosition)

            val deferred = async(Dispatchers.Main) {
                try {
                    if (isActive) {
                        val entries = refreshEntriesAsync(tabPosition).await()
                        tabFragment.setEntries(entries)
                    }
                    return@async
                }
                catch (e: Exception) {
                    Log.d("FailedToRefreshEntries", Log.getStackTraceString(e))
                    if (categoryChanged) {
                        tabFragment.clear()
                    }
                }
            }
            mTasks.add(deferred)
        }

        launch(Dispatchers.Main) {
            try {
                mTasks.awaitAll()
            }
            catch (e: Exception) {
                activity!!.showToast("エントリーリスト更新失敗")
            }
            finally {
                mTasks.clear()
                mainActivity.hideProgressBar()
            }
        }
    }

    private fun openFABMenuAnimation(layoutId: Int, descId: Int, dimenId: Int) {
        val layout = mView.findViewById<View>(layoutId)
        val desc = mView.findViewById<View>(descId)

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
        val layout = mView.findViewById<View>(layoutId)
        val desc = mView.findViewById<View>(descId)

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
            val menuBackground = mView.findViewById<View>(R.id.entries_menu_background_guard_full)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            menuBackground.visibility = View.VISIBLE
        }

        val clickGuard = mView.findViewById<View>(R.id.entries_menu_background_guard)
        clickGuard.visibility = View.VISIBLE

        if (HatenaClient.signedIn()) {
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

        val menuButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_button)
        menuButton.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeFABMenu() {
        if (!mIsFABMenuOpened) return

        mIsFABMenuOpened = false
        mView.findViewById<View>(R.id.entries_menu_background_guard_full).visibility = View.GONE
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        val clickGuard = mView.findViewById<View>(R.id.entries_menu_background_guard)
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

        val menuButton = mView.findViewById<FloatingActionButton>(R.id.entries_menu_button)
        menuButton.setImageResource(R.drawable.ic_baseline_menu_white)
    }
}
