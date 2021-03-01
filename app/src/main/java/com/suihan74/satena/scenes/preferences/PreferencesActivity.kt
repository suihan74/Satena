package com.suihan74.satena.scenes.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.suihan74.satena.PreferencesMigration
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityPreferencesBinding
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.backup.Credentials
import com.suihan74.satena.scenes.tools.RestartActivity
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getObjectExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime

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

    class ActivityViewModel : ViewModel() {
        val currentTab : MutableLiveData<PreferencesTabMode> by lazy {
            MutableLiveData(PreferencesTabMode.INFORMATION)
        }
    }

    val viewModel by lazyProvideViewModel { ActivityViewModel() }

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
        intent.getObjectExtra<PreferencesTabMode>(EXTRA_CURRENT_TAB)?.let {
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

    // ------- //

    private fun initializeContents() {
        // アイコンメニュー
        binding.menuRecyclerView.adapter = PreferencesMenuAdapter(this).also { adapter ->
            adapter.setOnClickListener {
                binding.preferencesViewPager.setCurrentItem(it.int, false)
            }

            viewModel.currentTab.observe(this, {
                adapter.selectTab(it)
            })
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
                        PreferencesTabMode.DUMMY_HEAD.int -> tabAdapter.getActualCount()
                        PreferencesTabMode.DUMMY_TAIL.int -> 1
                        else -> position
                    }

                    val prevTabId = viewModel.currentTab.value?.int
                    if (prevTabId != null) {
                        tabAdapter.findFragment(prevTabId).alsoAs<TabItem> { fragment ->
                            fragment.onTabUnselected()
                        }
                    }

                    val tab = PreferencesTabMode.fromId(jumpPosition)
                    viewModel.currentTab.value = tab

                    tabAdapter.findFragment(tab.int).alsoAs<TabItem> { fragment ->
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

        val position = tab.int
        binding.preferencesViewPager.setCurrentItem(position, false)
        title = getString(R.string.pref_toolbar_title, getString(tab.titleId))
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
}
