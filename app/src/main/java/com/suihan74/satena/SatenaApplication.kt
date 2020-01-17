package com.suihan74.satena

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresApi
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jakewharton.threetenabp.AndroidThreeTen
import com.suihan74.satena.models.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ServiceUtility
import com.suihan74.utilities.lock
import com.suihan74.utilities.showToast
import kotlinx.coroutines.runBlocking
import java.util.*

class SatenaApplication : Application() {
    companion object {
        lateinit var instance : SatenaApplication
            private set

        const val APP_DATABASE_FILE_NAME = "satena_db"
    }

    var currentActivity : ActivityBase? = null

    var isFirstLaunch : Boolean = false

    lateinit var appDatabase: AppDatabase
        private set

    val networkReceiver = NetworkReceiver(this)

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // initialize the timezone information
        AndroidThreeTen.init(applicationContext)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)

        // DBを準備する
        initializeDataBase()

        // DI
//        appComponent = DaggerAppComponent.builder()
//            .ignoredEntryModule(IgnoredEntryModule(this))
//            .build()

        // テーマの設定
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        setTheme(
            if (isThemeDark) R.style.AppTheme_Dark
            else R.style.AppTheme_Light
        )

        // GUIDの作成（初回のみ）
        val uuid = prefs.getString(PreferenceKey.ID)
        if (uuid.isNullOrEmpty()) {
            isFirstLaunch = true
            prefs.edit {
                putString(PreferenceKey.ID, UUID.randomUUID().toString())
            }
        }
        else {
            runBlocking {
                updatePreferencesVersion()
            }
        }

        // 接続状態を監視
        registerNetworkCallback()

        // 通知サービスを開始
        startNotificationService()
    }

    override fun onTerminate() {
        unregisterNetworkCallback()
        super.onTerminate()
    }

    /** ネットワークの接続状態を監視する */
    @RequiresApi(23)
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(request, networkReceiver.networkCallback)
    }

    @RequiresApi(23)
    private fun unregisterNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkReceiver.networkCallback)
    }

    /** DBを準備する */
    fun initializeDataBase() {
        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, APP_DATABASE_FILE_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    /** ユーザータグDBへのアクセスオブジェクトを取得 */
    val userTagDao
        get() = appDatabase.userTagDao()

    /** 非表示エントリDBへのアクセスオブジェクトを取得 */
    val ignoredEntryDao
        get() = appDatabase.ignoredEntryDao()

    /** 各種設定のバージョン移行が必要か確認 */
    suspend fun updatePreferencesVersion() {
        PreferenceKeyMigration.check(applicationContext)
        IgnoredEntriesKeyMigration.check(applicationContext)
        UserTagsKeyMigration.check(applicationContext)
    }

    /** 通知サービスを開始 */
    fun startNotificationService() {
        lock(this) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)
            val signedIn = prefs.contains(PreferenceKey.HATENA_USER_NAME)
            val isBackgroundCheckingNoticeEnabled = prefs.getBoolean(PreferenceKey.BACKGROUND_CHECKING_NOTICES)

            if (signedIn && isBackgroundCheckingNoticeEnabled && !NotificationService.running) {
                val intent = Intent(this, NotificationService::class.java)
                ServiceUtility.start(this, intent)

                showToast("常駐してはてなの通知をおしらせします")
            }
        }
    }

    /** 通知サービスを明示的に終了 */
    fun stopNotificationService() {
        lock(this) {
            val intent = Intent(this, NotificationService::class.java)
            applicationContext.stopService(intent)
        }
    }
}
