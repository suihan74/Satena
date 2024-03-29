package com.suihan74.satena.scenes.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.PreferencesMigration
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityPreferencesBinding
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.backup.Credentials
import com.suihan74.satena.scenes.preferences.ignored.FollowingUsersViewModel
import com.suihan74.satena.scenes.preferences.ignored.PreferencesIgnoredUsersViewModel
import com.suihan74.satena.scenes.preferences.ignored.UserRelationRepository
import com.suihan74.satena.scenes.preferences.pages.*
import com.suihan74.satena.scenes.tools.RestartActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.TabItem
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.time.LocalDateTime

class PreferencesActivity : AppCompatActivity() {
    enum class RequestCode {
        WRITE,
        READ
    }

    companion object {
        const val EXTRA_THEME_CHANGED = "theme_changed"
        const val EXTRA_CURRENT_TAB = "current_tab"
        const val EXTRA_RELOAD_ALL_PREFERENCES = "reload_all_preferences"
    }

    // ------ //

    /** 項目の検索結果 */
    data class SearchResultItem(
        val item : PreferencesAdapter.Item,
        val tab : PreferencesTab
    )

    // ------ //

    class ActivityViewModel : ViewModel() {
        val currentTab : MutableLiveData<PreferencesTab> by lazy {
            MutableLiveData(PreferencesTab.INFORMATION)
        }

        val searchText = MutableStateFlow("")

        /** 検索用の全設定リスト */
        val allPreferencesList = MutableStateFlow<List<SearchResultItem>>(emptyList())

        val filteredPreferencesList = MutableLiveData<List<SearchResultItem>>()

        // ------ //

        init {
            combine(allPreferencesList, searchText, ::Pair)
                .onEach { (rawList, query) ->
                    if (query.isBlank()) {
                        filteredPreferencesList.postValue(emptyList())
                    }
                    else {
                        val regex = Regex(
                            query.split(Regex("""\s+""")).joinToString("","^",".*$") { s -> "(?=.*\\Q$s\\E)" },
                            RegexOption.IGNORE_CASE
                        )

                        filteredPreferencesList.postValue(
                            rawList
                                .filter { regex.find(it.item.description) != null }
                                .filter { it.item !is PreferencesAdapter.Section }
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    val viewModel by lazyProvideViewModel { ActivityViewModel() }

    val informationViewModel by lazyProvideViewModel { InformationViewModel(this) }

    val accountViewModel by lazyProvideViewModel { AccountViewModel(this, SatenaApplication.instance.accountLoader) }

    val generalViewModel by lazyProvideViewModel { GeneralViewModel(this) }

    val entryViewModel by lazyProvideViewModel { EntryViewModel(this) }

    val bookmarkViewModel by lazyProvideViewModel { BookmarkViewModel(this, SatenaApplication.instance.accountLoader) }

    val browserViewModel by lazyProvideViewModel {
        BrowserViewModel(this).apply {
            val context = this@PreferencesActivity
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val browserSettings = SafeSharedPreferences.create<BrowserSettingsKey>(context)

            browserRepo = BrowserRepository(HatenaClient, prefs, browserSettings)
            historyRepo = HistoryRepository(browserSettings, SatenaApplication.instance.browserDao)
        }
    }

    /** ユーザー関係のリポジトリ */
    private val userRelationRepository by lazy {
        UserRelationRepository(SatenaApplication.instance.accountLoader)
    }

    private val ignoredUsersViewModelDelegate = lazyProvideViewModel { PreferencesIgnoredUsersViewModel(userRelationRepository) }
    /** 非表示ユーザー用ViewModel */
    val ignoredUsersViewModel by ignoredUsersViewModelDelegate

    private val followingsViewModelDelegate = lazyProvideViewModel { FollowingUsersViewModel(userRelationRepository) }
    /** フォロー/フォロワー用ViewModel */
    val followingsViewModel by followingsViewModelDelegate

    // ------ //

    /** はてなにサインイン完了した際にユーザー情報を再読み込みする */
    val hatenaAuthenticationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (followingsViewModelDelegate.isInitialized()) {
                lifecycleScope.launch {
                    followingsViewModel.loadList(refreshAll = true)
                }
            }
            if (ignoredUsersViewModelDelegate.isInitialized()) {
                lifecycleScope.launch {
                    ignoredUsersViewModel.loadList()
                }
            }
        }
    }

    // ------ //

    private lateinit var binding: ActivityPreferencesBinding

    private var themeChanged : Boolean = false

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(Theme.themeId(prefs))
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.preferencesToolbar)

        themeChanged = intent.getBooleanExtra(EXTRA_THEME_CHANGED, false)

        // 初期タブが指定されている場合
        intent.getObjectExtra<PreferencesTab>(EXTRA_CURRENT_TAB)?.let {
            viewModel.currentTab.value = it
        }

        val invokeReload = intent.getBooleanExtra(EXTRA_RELOAD_ALL_PREFERENCES, false)
        if (invokeReload) {
            showProgressBar()
            lifecycleScope.launch(Dispatchers.Main) {
                reloadAllPreferences()
                initializeContents()
                hideProgressBar()
            }
        }
        else {
            initializeContents()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.preferences_activity, menu)
        menu?.findItem(R.id.search_view)?.actionView.alsoAs<SearchView> { searchView ->
            val backPressedCallback =
                onBackPressedDispatcher.addCallback(
                    this,
                    false
                ) {
                    searchView.setQuery("", false)
                    searchView.isIconified = true
                    isEnabled = false
                }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                private var job : Job? = null
                override fun onQueryTextChange(newText: String?): Boolean {
                    job?.cancel()
                    job = lifecycleScope.launch(Dispatchers.Default) {
                        delay(800L)
                        viewModel.searchText.value = newText.orEmpty()
                        job = null
                    }
                    return true
                }
                override fun onQueryTextSubmit(query: String?): Boolean {
                    job?.cancel()
                    job = null
                    hideSoftInputMethod()
                    searchView.clearFocus()
                    if (viewModel.searchText.value != query) {
                        viewModel.searchText.value = query.orEmpty()
                    }
                    return true
                }
            })
            searchView.setOnSearchClickListener {
                backPressedCallback.isEnabled = true
                viewModel.searchText.value = searchView.query?.toString().orEmpty()
            }
            searchView.setOnCloseListener {
                viewModel.searchText.value = ""
                backPressedCallback.isEnabled = false
                false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    // ------- //

    private fun initializeContents() {
        viewModel.allPreferencesList.value = createAllPreferencesList()

        // アイコンメニュー
        binding.menuRecyclerView.adapter = PreferencesMenuAdapter(this).also { adapter ->
            adapter.setOnClickListener {
                binding.preferencesViewPager.setCurrentItem(it.ordinal, false)
            }

            viewModel.currentTab.observe(this) {
                adapter.selectTab(it)
            }
        }

        // ページャ
        val tabAdapter = PreferencesTabAdapter(this)
        binding.preferencesViewPager.also { pager ->
            // 環状スクロールできるように細工
            pager.adapter = tabAdapter
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                private var jumpPosition = -1

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    jumpPosition = when (position) {
                        PreferencesTab.DUMMY_HEAD.ordinal -> tabAdapter.getActualCount()
                        PreferencesTab.DUMMY_TAIL.ordinal -> 1
                        else -> position
                    }

                    val prevTabId = viewModel.currentTab.value?.ordinal
                    if (prevTabId != null) {
                        tabAdapter.findFragment(prevTabId).alsoAs<TabItem> { fragment ->
                            fragment.onTabUnselected()
                        }
                    }

                    val tab = PreferencesTab.fromOrdinal(jumpPosition)
                    viewModel.currentTab.value = tab

                    tabAdapter.findFragment(tab.ordinal).alsoAs<TabItem> { fragment ->
                        fragment.onTabSelected()
                    }

                    title = getString(R.string.pref_toolbar_title, getString(tab.titleId))
                }
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager.SCROLL_STATE_IDLE && jumpPosition > 0) {
                        pager.setCurrentItem(jumpPosition, false)
                        jumpPosition = -1
                    }
                }
            })
        }

        val tab = viewModel.currentTab.value!!

        val position = tab.ordinal
        binding.preferencesViewPager.setCurrentItem(position, false)
        title = getString(R.string.pref_toolbar_title, getString(tab.titleId))

        // 検索結果
        binding.searchResultRecyclerView.apply {
            adapter = preferencesAdapterForSearchResult(
                this@PreferencesActivity,
                binding.preferencesViewPager
            ).also { adapter ->
                viewModel.filteredPreferencesList.observe(this@PreferencesActivity) { items ->
                    binding.searchResultClickGuard.setVisibility(items.isNotEmpty())
                    adapter.submitList(items.map { it.item })
                }
            }
        }

        binding.searchResultClickGuard.setOnClickListener {
            viewModel.searchText.value = ""
        }
    }

    private fun preferencesAdapterForSearchResult(
        activity: PreferencesActivity,
        viewPager: ViewPager2
    ) = PreferencesAdapter(activity).also { adapter ->
        adapter.overrideItemClickListener { item ->
            val searchResult =
                viewModel.filteredPreferencesList.value?.firstOrNull { it.item == item }
                    ?: return@overrideItemClickListener
            viewModel.searchText.value = ""

            if (viewPager.currentItem == searchResult.tab.ordinal) {
                viewPager.adapter.alsoAs<PreferencesTabAdapter> { adapter ->
                    adapter.findFragment(searchResult.tab.ordinal).alsoAs<ListPreferencesFragment> {
                        it.scrollTo(item)
                    }
                }
            }
            else {
                // ページ切替後にスクロール
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    private var isEnabled = true
                    override fun onPageScrollStateChanged(state: Int) {
                        if (!isEnabled) return
                        if (state != ViewPager2.SCROLL_STATE_IDLE) return
                        val tabAdapter = viewPager.adapter as? PreferencesTabAdapter ?: return
                        isEnabled = false
                        tabAdapter.findFragment(searchResult.tab.ordinal)
                            .alsoAs<ListPreferencesFragment> {
                                it.lifecycleScope.launchWhenResumed {
                                    delay(150L)
                                    it.scrollTo(item)
                                }
                            }
                    }
                })
                viewPager.setCurrentItem(searchResult.tab.ordinal, true)
            }
        }
    }

    // ------ //

    private fun showProgressBar() {
        binding.clickGuard.visibility = View.VISIBLE
        binding.detailProgressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.clickGuard.visibility = View.GONE
        binding.detailProgressBar.visibility = View.GONE
    }
    // ------ //

    /** ファイルから設定を読み込んだ場合，このメソッドを使用して変更内容を適用する */
    private suspend fun reloadAllPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        try {
            // 再ログイン
            SatenaApplication.instance.accountLoader.signInAccounts(true)
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
        try {
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return
            }

            if (themeChanged) {
                val intent = Intent(this, EntriesActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            else {
                backActivity()
            }
        }
        catch (e: Throwable) {
            Log.e("onBackPressed", Log.getStackTraceString(e))
            backActivity()
        }
    }

    private fun backActivity() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
            overridePendingTransition(0, android.R.anim.slide_out_right)
        }
        else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    // ------ //

    private val writerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onFinishSettingDialog(RequestCode.WRITE, result)
    }

    private val readerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onFinishSettingDialog(RequestCode.READ, result)
    }

    fun openSaveSettingsDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "${LocalDateTime.now()}.satena-settings")
        }
        writerLauncher.launch(intent)
    }

    fun openLoadSettingsDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        readerLauncher.launch(intent)
    }

    private fun contentFilePath(context: Context, uri: Uri) : String {
        val contentResolver = context.contentResolver
        val columns = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        return contentResolver.query(uri, columns, null, null, null).use { cursor ->
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                cursor.getString(0)
            }
            else uri.path!!
        }
    }

    private fun savePreferencesToFile(targetUri: Uri) {
        showProgressBar()

        lifecycleScope.launch(Dispatchers.Default) {
            val context = SatenaApplication.instance.applicationContext
            val credentials = Credentials.extract(context)

            try {
                PreferencesMigration.Output(context).run {
                    addPreference<PreferenceKey>()
                    addPreference<NoticesKey>()
                    addPreference<EntriesHistoryKey>()
                    addPreference<BrowserSettingsKey>()
                    addPreference<FavoriteSitesKey>()
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME)
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-shm")
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-wal")
                    addFiles(File(context.filesDir, "favicon_cache"))
                    write(targetUri)
                }

                withContext(Dispatchers.Main) {
                    val path = contentFilePath(context, targetUri)
                    context.showToast(R.string.msg_pref_information_save_succeeded, path)
                }
            }
            catch (e: Throwable) {
                Log.e("SavingSettings", e.message ?: "")
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.msg_pref_information_save_failed)
                }
            }
            finally {
                credentials.restore(context)
                withContext(Dispatchers.Main) {
                    hideProgressBar()
                }
            }
        }
    }

    private fun loadPreferencesFromFile(targetUri: Uri) {
        showProgressBar()

        lifecycleScope.launch(Dispatchers.Default) {
            val context = SatenaApplication.instance.applicationContext
            val credentials = Credentials.extract(context)

            try {
                PreferencesMigration.Input(context)
                    .read(targetUri)

                withContext(Dispatchers.Main) {
                    Log.i("satena", "restarting...")
                    val path = contentFilePath(context, targetUri)
                    context.showToast(R.string.msg_pref_information_load_succeeded, path)

                    // アプリを再起動
                    val intent = RestartActivity.createIntent(this@PreferencesActivity)
                    startActivity(intent)
                }
            }
            catch (e: PreferencesMigration.MigrationFailureException) {
                val msg = e.message ?: ""
                Log.e("LoadingSettings", msg)
                withContext(Dispatchers.Main) {
                    if (e.cause is IllegalStateException) {
                        context.showToast("${getString(R.string.msg_pref_information_load_failed)}\n${msg}")
                    }
                    else {
                        context.showToast(R.string.msg_pref_information_load_failed)
                    }
                }
            }
            finally {
                credentials.restore(context)
                withContext(Dispatchers.Main) {
                    hideProgressBar()
                }
            }
        }
    }

    private fun onFinishSettingDialog(requestCode: RequestCode, result: ActivityResult) {
        val targetUri = result.data?.data

        if (result.resultCode != Activity.RESULT_OK || targetUri == null) {
            Log.d("FilePick", "canceled")
            return
        }

        when (requestCode) {
            RequestCode.WRITE -> {
                Log.d("SaveSettings", targetUri.path ?: "")
                savePreferencesToFile(targetUri)
            }

            RequestCode.READ -> {
                Log.d("LoadSettings", targetUri.path ?: "")
                loadPreferencesFromFile(targetUri)
            }
        }
    }

    private fun createAllPreferencesList() : List<SearchResultItem> = buildList {
        fun MutableList<SearchResultItem>.addAll(tab: PreferencesTab, items: List<PreferencesAdapter.Item>) {
            this.addAll(items.map { SearchResultItem(item = it, tab = tab) })
        }
        val context = this@PreferencesActivity
        val fm = supportFragmentManager
        addAll(PreferencesTab.INFORMATION, informationViewModel.createList(context, fm))
        addAll(PreferencesTab.ACCOUNT, accountViewModel.createList(context, fm))
        addAll(PreferencesTab.GENERALS, generalViewModel.createList(context, fm))
        addAll(PreferencesTab.ENTRIES, entryViewModel.createList(context, fm))
        addAll(PreferencesTab.BOOKMARKS, bookmarkViewModel.createList(context, fm))
        addAll(PreferencesTab.BROWSER, browserViewModel.createList(context, fm))
    }
}
