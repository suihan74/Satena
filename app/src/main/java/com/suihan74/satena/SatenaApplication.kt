package com.suihan74.satena

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.*
import com.suihan74.satena.notices.NotificationWorker
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.checkRunningByTag
import com.suihan74.utilities.extensions.whenFalse
import com.suihan74.utilities.extensions.whenTrue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class SatenaApplication : Application() {
    companion object {
        lateinit var instance : SatenaApplication
            private set

        const val APP_DATABASE_FILE_NAME = "satena_db"
        const val WORKER_TAG_CHECKING_NOTICES = "WORKER_TAG_CHECKING_NOTICES"
    }

    // ------ //

    var isFirstLaunch : Boolean = false

    lateinit var appDatabase: AppDatabase
        private set

    lateinit var networkReceiver : NetworkReceiver
        private set

    // ------ //

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

    // ------ //
    // 基本的にどの画面を開いても使用するリポジトリはSatenaApplicationのインスタンスが保持する

    /** 非表示URL/TEXT情報を扱うリポジトリ */
    lateinit var ignoredEntriesRepository : IgnoredEntriesRepository
        private set

    /** お気に入りサイトを扱うリポジトリ */
    lateinit var favoriteSitesRepository : FavoriteSitesRepository

    /** アカウント管理 */
    val accountLoader : AccountLoader by lazy {
        AccountLoader(this, HatenaClient, MastodonClientHolder)
    }

    // ------ //

    /** ユーザータグDBへのアクセスオブジェクトを取得 */
    val userTagDao
        get() = appDatabase.userTagDao()

    /** 非表示エントリDBへのアクセスオブジェクトを取得 */
    private val ignoredEntryDao
        get() = appDatabase.ignoredEntryDao()
    // 全処理をignoredEntriesRepositoryを介してアクセスするようにしたので、外部には非公開に変更

    /** 内部ブラウザ用DB */
    val browserDao
        get() = appDatabase.browserDao()

    // ------ //

    override fun onCreate() {
        super.onCreate()

        if (!isMainProcess(this)) {
            // 別プロセスでの起動は`RestartActivity`でのみ使用する
            // このとき通常の`SatenaApplication`初期化処理は必要ないので行わない
            return
        }

        // デバッグビルドでクラッシュレポートを送信しない
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(
                BuildConfig.DEBUG.not()
            )

        instance = this

        // initialize the timezone information
        AndroidThreeTen.init(applicationContext)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)

        // DBを準備する
        initializeDataBase()

        // ネットワーク接続状態の監視者
        networkReceiver = NetworkReceiver(this)

        // テーマの設定
        setTheme(Theme.themeId(prefs))

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

        // グローバルなリポジトリを初期化する
        initializeRepositories()

        // 接続状態を監視
        registerNetworkCallback()

        // バックグラウンドでの通知確認を開始
        startCheckingNotificationsWorker(applicationContext)
    }

    override fun onTerminate() {
        unregisterNetworkCallback()
        super.onTerminate()
    }

    // ------ //

    /** 各種グローバルリポジトリを生成 */
    private fun initializeRepositories() {
        ignoredEntriesRepository = IgnoredEntriesRepository(ignoredEntryDao).also {
            GlobalScope.launch {
                it.loadAllIgnoredEntries()
            }
        }

        favoriteSitesRepository = FavoriteSitesRepository(
            SafeSharedPreferences.create(this),
            HatenaClient
        )
    }

    // ------ //

    /** ネットワークの接続状態を監視する */
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(request, networkReceiver.networkCallback)
    }

    private fun unregisterNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkReceiver.networkCallback)
    }

    // ------ //

    /** DBを準備する */
    fun initializeDataBase() {
        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, APP_DATABASE_FILE_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .migrate()
            .build()
    }

    /** 各種設定のバージョン移行が必要か確認 */
    fun updatePreferencesVersion() {
        PreferenceKeyMigration.check(applicationContext)
    }

    // ------ //

    /** 通知確認をバックグラウンドで開始 */
    fun startCheckingNotificationsWorker(context: Context, forceReplace: Boolean = false) {
        val result = runCatching {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
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
                PeriodicWorkRequestBuilder<NotificationWorker>(
                    interval, TimeUnit.MINUTES,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
                )
                .setConstraints(constraints)
                .addTag(WORKER_TAG_CHECKING_NOTICES)
                .build()

            WorkManager.getInstance(context).let { manager ->
                manager.checkRunningByTag(WORKER_TAG_CHECKING_NOTICES).whenFalse {
                    showToast(R.string.msg_start_checking_notifications)
                }

                manager.enqueueUniquePeriodicWork(
                    WORKER_TAG_CHECKING_NOTICES,
                    if (forceReplace) ExistingPeriodicWorkPolicy.REPLACE
                    else ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }

            Log.i("WorkManager", "start checking notifications")
        }

        if (result.isFailure) {
            showToast(R.string.msg_start_checking_notifications_failed)
            Log.e("WorkManager", "checking notifications failure")
        }
    }

    /** 通知確認を明示的に終了 */
    fun stopCheckingNotificationsWorker(context: Context) {
        runCatching {
            WorkManager.getInstance(context).let { manager ->
                manager.checkRunningByTag(WORKER_TAG_CHECKING_NOTICES).whenTrue {
                    showToast(R.string.msg_stop_checking_notifications)
                }
                manager.cancelAllWorkByTag(WORKER_TAG_CHECKING_NOTICES)
            }
            Log.i("WorkManager", "stop checking notifications")
        }
    }

    // ------ //

    /**
     * メインプロセスで実行されているかを確認する
     *
     * @throws NullPointerException `ActivityManager`が取得できない
     */
    private fun isMainProcess(context: Context) : Boolean {
        val manager = context.getSystemService<ActivityManager>()!!
        val pid = android.os.Process.myPid()
        return manager.runningAppProcesses.firstOrNull {
            it.processName == BuildConfig.APPLICATION_ID && it.pid == pid
        } != null
    }

    // ------ //

    /** v1.5.11まで使用していた常駐通知チャンネルを削除する */
    private fun deleteOldNotificationChannel(prefs: SafeSharedPreferences<PreferenceKey>) {
        runCatching {
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
}
