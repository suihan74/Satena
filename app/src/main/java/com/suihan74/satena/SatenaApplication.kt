package com.suihan74.satena

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.room.Room
import com.jakewharton.threetenabp.AndroidThreeTen
import com.suihan74.satena.models.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ServiceUtility
import com.suihan74.utilities.lock
import com.suihan74.utilities.showToast
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

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // initialize the timezone information
        AndroidThreeTen.init(applicationContext)

        // 接続状態を監視
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

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
        if (isThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }
        else {
            setTheme(R.style.AppTheme_Light)
        }

        // GUIDの作成（初回のみ）
        val uuid = prefs.getString(PreferenceKey.ID)
        if (uuid.isNullOrEmpty()) {
            isFirstLaunch = true
            prefs.edit {
                putString(PreferenceKey.ID, UUID.randomUUID().toString())
            }
        }
        else {
            updatePreferencesVersion()
        }

        startNotificationService()
    }

    fun initializeDataBase() {
        // DBを準備する
        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, APP_DATABASE_FILE_NAME)
            .build()
    }

    /** ユーザータグDBへのアクセスオブジェクトを取得 */
    val userTagDao
        get() = appDatabase.userTagDao()

    /** 非表示エントリDBへのアクセスオブジェクトを取得 */
    val ignoredEntryDao
        get() = appDatabase.ignoredEntryDao()

    /** 各種設定のバージョン移行が必要か確認 */
    fun updatePreferencesVersion() {
        PreferenceKeyMigrator.check(applicationContext)
        IgnoredEntriesKeyMigrator.check(applicationContext)
        UserTagsKeyMigrator.check(applicationContext)
    }

    fun setConnectionActivatingListener(listener: (()->Unit)?) {
        ConnectivityReceiver.setConnectionActivatingListener(listener)
    }

    fun setConnectionActivatedListener(listener: (()->Unit)?) {
        ConnectivityReceiver.setConnectionActivatedListener(listener)
    }

    fun setConnectionDeactivatedListener(listener: (()->Unit)?) {
        ConnectivityReceiver.setConnectionDeactivatedListener(listener)
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
