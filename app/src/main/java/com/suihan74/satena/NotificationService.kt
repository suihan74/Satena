package com.suihan74.satena

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Notice
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.entries.notices.NoticesFragment
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.makeSpannedfromHtml
import kotlinx.coroutines.*
import org.threeten.bp.LocalDateTime
import kotlin.coroutines.CoroutineContext

class NotificationService : Service(), CoroutineScope {
    companion object {
        private const val SERVICE_CHANNEL_ID = "satena_notification_service"
        private const val NOTICE_CHANNEL_ID = "satena_notification"

        var running : Boolean = false
            get() = synchronized(field) { field }
            private set(value) {
                synchronized(field) {
                    field = value
                }
            }

        private var runningJob : Job? = null
            get() = synchronized(this) { field }
            private set(value) {
                synchronized(this) {
                    field = value
                }
            }
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job


    override fun onCreate() {
        super.onCreate()
        running = true

        Log.d("NotificationService", "created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationService", "starting service...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val description = "はてなの通知を確認します"

            // グループ生成
            val createGroup = { groupId: String, groupName: String ->
                NotificationChannelGroup(groupId, groupName)
            }
            notificationManager.createNotificationChannelGroups(listOf(
                createGroup(SERVICE_CHANNEL_ID, "通知確認サービス"),
                createGroup(NOTICE_CHANNEL_ID, "通知")
            ))

            // チャンネル生成
            // 通知確認サービス常駐用
            if (notificationManager.getNotificationChannel(SERVICE_CHANNEL_ID) != null) {
                notificationManager.deleteNotificationChannel(SERVICE_CHANNEL_ID)
            }
            val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, "通知確認サービス", NotificationManager.IMPORTANCE_HIGH).apply {
                group = SERVICE_CHANNEL_ID
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // 通知
            if (notificationManager.getNotificationChannel(NOTICE_CHANNEL_ID) != null) {
                notificationManager.deleteNotificationChannel(NOTICE_CHANNEL_ID)
            }
            val noticeChannel = NotificationChannel(NOTICE_CHANNEL_ID, "通知", NotificationManager.IMPORTANCE_DEFAULT).apply {
                group = NOTICE_CHANNEL_ID
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
//                vibrationPattern = longArrayOf(0, 300, 300)
                enableVibration(true)
                enableLights(true)
//                    setShowBadge(true)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(noticeChannel)

            val mainIntent = Intent(applicationContext, EntriesActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(applicationContext, SERVICE_CHANNEL_ID)
                .setGroup(SERVICE_CHANNEL_ID)
                .setStyle(NotificationCompat.BigTextStyle().setSummaryText(description))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build()

            startForeground(1, notification)
        }

        startFetchingNotices()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        running = false
        job.cancel()
        Log.d("NotificationService", "destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startFetchingNotices() {
        if (runningJob != null) {
            runningJob!!.cancel()
        }

        runningJob = launch (Dispatchers.IO) {
            while (true) {
                fetchNotices(applicationContext)
                Log.d("NotificationService", "fetched")

                val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)
                val minutes = prefs.getLong(PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS)

                delay(1000 * 60 * minutes)
            }
        }
    }

    private suspend fun fetchNotices(context: Context) {
        try {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val isLastSeenUpdatable = prefs.getBoolean(PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE)
            val localLastUpdated = prefs.getNullable<LocalDateTime>(PreferenceKey.NOTICES_LAST_SEEN)
            val now = LocalDateTime.now()

            AccountLoader.signInHatenaAsync(context, reSignIn = false).await()

            val response = HatenaClient.getNoticesAsync().await()
            if (isLastSeenUpdatable) {
                try {
                    HatenaClient.updateNoticesLastSeenAsync().start()
                }
                catch (e: Exception) {
                    Log.e("failedToUpdateLastSeen", Log.getStackTraceString(e))
                }
            }
            prefs.edit {
                putObject(PreferenceKey.NOTICES_LAST_SEEN, now)
            }

            if (localLastUpdated == null) {
                // 初回起動時は通知しない
                return
            }

            for (notice in response.notices) {
                if (notice.modified > localLastUpdated) {
                    invokeNotice(context, notice)
                    Log.d("NotificationService", "new notice: ${notice.verb}")
                }
            }
        }
        catch (e: Exception) {
            Log.e("failedToFetchNotices", Log.getStackTraceString(e))
        }
    }

    private fun invokeNotice(context: Context, notice: Notice) {
        val title = "通知"
        val message = NoticesFragment.createMessage(notice, context).let {
            makeSpannedfromHtml(it).toString()
        }

        var actions : List<NotificationCompat.Action>? = null
        val intent = when (notice.verb) {
            Notice.VERB_STAR -> {
                val openEntryIntent = Intent(context, BookmarksActivity::class.java).apply {
                    putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
                }

                val openBookmarkIntent = Intent(context, BookmarksActivity::class.java).apply {
                    putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
                    putExtra(BookmarksActivity.EXTRA_TARGET_USER, HatenaClient.account!!.name)
                }

                val openNoticesIntent = Intent(context, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_NOTICES, true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }

                actions = listOf(
                    NotificationCompat.Action(0, "エントリを開く", PendingIntent.getActivity(context, 1, openEntryIntent, PendingIntent.FLAG_UPDATE_CURRENT)),
                    NotificationCompat.Action(0, "ブコメを見る", PendingIntent.getActivity(context, 2, openBookmarkIntent, PendingIntent.FLAG_UPDATE_CURRENT)),
                    NotificationCompat.Action(0, "通知一覧", PendingIntent.getActivity(context, 3, openNoticesIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                )

                // デフォルトで開くのはブコメ詳細
                openBookmarkIntent
            }

            Notice.VERB_ADD_FAVORITE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
            }

            else -> {
                Intent(context, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_NOTICES, true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val style = NotificationCompat.BigTextStyle()
            .bigText(message)

        val builder = NotificationCompat.Builder(context, NOTICE_CHANNEL_ID)
            .setGroup(NOTICE_CHANNEL_ID)
            .setStyle(style)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        actions?.forEach {
            builder.addAction(it)
        }

        NotificationManagerCompat.from(context).notify(0, builder.build())
    }
}

