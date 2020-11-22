package com.suihan74.satena

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.suihan74.satena.models.AppDatabase
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.PreferenceKeyMigration
import com.suihan74.satena.models.migrate
import com.suihan74.satena.notices.NotificationWorker
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.checkRunningByTag
import com.suihan74.utilities.extensions.showToast
import java.util.*
import java.util.concurrent.TimeUnit

class SatenaApplication : Application() {
    companion object {
        lateinit var instance : SatenaApplication
            private set

        const val APP_DATABASE_FILE_NAME = "satena_db"
        const val WORKER_TAG_CHECKING_NOTICES = "WORKER_TAG_CHECKING_NOTICES"
    }

    var isFirstLaunch : Boolean = false

    lateinit var appDatabase: AppDatabase
        private set

    val networkReceiver = NetworkReceiver(this)

    init {
        instance = this
    }

    /** アプリのバージョン番号 */
    val versionCode: Long by lazy {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    /** アプリのバージョン名 */
    val versionName: String by lazy {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName
    }

    /** アプリのメジャーバージョン */
    val majorVersionCode: Long by lazy { getMajorVersion(versionCode) }

    /** アプリのマイナーバージョン */
    val minorVersionCode: Long by lazy { getMinorVersion(versionCode) }

    /** アプリの修正バージョン */
    val fixVersionCode: Long by lazy { getFixVersion(versionCode) }

    /** アプリの開発バージョン */
    val developVersionCode: Long by lazy { getDevelopVersion(versionCode) }

    /** バージョンコード値からメジャーバージョンを計算する */
    fun getMajorVersion(versionCode: Long) : Long =
        versionCode / 100000000

    /** バージョンコード値からマイナーバージョンを計算する */
    fun getMinorVersion(versionCode: Long) : Long {
        val upperMask = 100000000
        val lowerMask = 1000000
        return (versionCode % upperMask) / lowerMask
    }

    /** バージョンコード値から修正バージョンを計算する */
    fun getFixVersion(versionCode: Long) : Long {
        val upperMask = 1000000
        val lowerMask = 1000
        return (versionCode % upperMask) / lowerMask
    }

    /** バージョンコード値から修正バージョンを計算する */
    fun getDevelopVersion(versionCode: Long) : Long =
        versionCode / 1000

    override fun onCreate() {
        super.onCreate()

        // initialize the timezone information
        AndroidThreeTen.init(applicationContext)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)

        // DBを準備する
        initializeDataBase()

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
            // 旧バージョンで使用していた通知チャンネルを削除する
            deleteOldNotificationChannel(prefs)

            // 設定ファイルのバージョン移行が必要ならする
            updatePreferencesVersion()
        }

        // 接続状態を監視
        registerNetworkCallback()

        // バックグラウンドでの通知確認を開始
        startCheckingNotificationsWorker()
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
            .migrate()
            .build()
    }

    /** ユーザータグDBへのアクセスオブジェクトを取得 */
    val userTagDao
        get() = appDatabase.userTagDao()

    /** 非表示エントリDBへのアクセスオブジェクトを取得 */
    val ignoredEntryDao
        get() = appDatabase.ignoredEntryDao()

    /** 内部ブラウザ用DB */
    val browserDao
        get() = appDatabase.browserDao()

    /** 各種設定のバージョン移行が必要か確認 */
    fun updatePreferencesVersion() {
        PreferenceKeyMigration.check(applicationContext)
    }

    /** 通知確認をバックグラウンドで開始 */
    fun startCheckingNotificationsWorker(forceRestart: Boolean = false) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val signedIn = prefs.contains(PreferenceKey.HATENA_USER_NAME)
        val enabled = prefs.getBoolean(PreferenceKey.BACKGROUND_CHECKING_NOTICES)
        val interval = prefs.getLong(PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS)

        if (!signedIn || !enabled) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<NotificationWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORKER_TAG_CHECKING_NOTICES)
                .build()

        WorkManager.getInstance(this).let { manager ->
            val existed = manager.checkRunningByTag(WORKER_TAG_CHECKING_NOTICES)

            if (forceRestart || !existed) {
                manager.cancelAllWorkByTag(WORKER_TAG_CHECKING_NOTICES)
                manager.enqueue(workRequest)
                showToast(R.string.msg_start_checking_notifications)
            }
        }

        Log.i("WorkManager", "start checking notifications")
    }

    /** 通知確認を明示的に終了 */
    fun stopCheckingNotificationsWorker() {
        WorkManager.getInstance(this).let { manager ->
            val existed = manager.checkRunningByTag(WORKER_TAG_CHECKING_NOTICES)
            manager.cancelAllWorkByTag(WORKER_TAG_CHECKING_NOTICES)

            if (existed) {
                showToast(R.string.msg_stop_checking_notifications)
            }
        }

        Log.i("WorkManager", "stop checking notifications")
    }

    // ------ //

    /** v1.5.11まで使用していた常駐通知チャンネルを削除する */
    private fun deleteOldNotificationChannel(prefs: SafeSharedPreferences<PreferenceKey>) {
        if (prefs.version >= 5) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val serviceChannelId = "satena_notification_service"
        val manager = NotificationManagerCompat.from(this)
        if (manager.getNotificationChannel(serviceChannelId) != null) {
            manager.deleteNotificationChannel(serviceChannelId)
        }
        if (manager.getNotificationChannelGroup(serviceChannelId) != null) {
            manager.deleteNotificationChannelGroup(serviceChannelId)
        }
    }
}
