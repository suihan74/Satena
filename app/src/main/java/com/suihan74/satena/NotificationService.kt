package com.suihan74.satena

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Notice
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.activities.MainActivity
import com.suihan74.satena.fragments.NoticesFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.coroutines.*
import org.threeten.bp.LocalDateTime
import kotlin.coroutines.CoroutineContext

class NotificationService : Service(), CoroutineScope {
    companion object {
        var running : Boolean = false
            get() {
                synchronized(field) {
                    return field
                }
            }
            private set(value) {
                synchronized(field) {
                    field = value
                }
            }

        private var runningJob : Job? = null
            get() {
                synchronized(this) {
                    return field
                }
            }
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
            val channelId = "satena_notification_service"
            val title = "Satena"
            val description = "はてなの通知を確認します"

            val mainIntent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setStyle(NotificationCompat.BigTextStyle().setSummaryText(description))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build()

            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    vibrationPattern = longArrayOf(0L)
                    enableVibration(false)
                }

                notificationManager.createNotificationChannel(channel)
            }
            startForeground(1, notification)
        }

        startFetchingNotices(applicationContext)

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

    private fun startFetchingNotices(context: Context) {
        context.showToast("常駐してはてなの通知をおしらせします")

        if (runningJob != null) {
            runningJob!!.cancel()
        }

        runningJob = launch {
            while (true) {
                fetchNotices(context)
                Log.d("NotificationService", "fetched")

                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
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
        val title = "Satena"
        val channelId = "satena_notification"
        val message = NoticesFragment.createMessage(notice, context).let {
            makeSpannedfromHtml(it).toString()
        }

        val intent = when (notice.verb) {
            Notice.VERB_STAR -> {
                Intent(context, BookmarksActivity::class.java).apply {
                    putExtra("eid", notice.eid)
                }
            }

            Notice.VERB_ADD_FAVORITE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
            }

            else -> {
                Intent(context, MainActivity::class.java).apply {
                    putExtra("fragment", NoticesFragment::class.java)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val style = NotificationCompat.BigTextStyle()
            .bigText(message)

        val notification = NotificationCompat.Builder(context, channelId)
            .setStyle(style)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(0, notification)
    }
}

