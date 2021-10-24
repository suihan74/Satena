package com.suihan74.satena.notices

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.checkFromSpam
import com.suihan74.utilities.extensions.makeSpannedFromHtml
import com.suihan74.utilities.extensions.message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * バックグラウンドで反復実行される通知確認処理
 */
class NotificationWorker(applicationContext: Context, workerParameters: WorkerParameters)
    : CoroutineWorker(applicationContext, workerParameters)
{
    companion object {
        private const val NOTICE_CHANNEL_ID = "satena_notification"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i("NotificationWorker", "check notifications")
        runCatching {
            fetchNotices(applicationContext)
        }
        Result.success()
    }

    /** 最新の通知リストを取得する */
    private suspend fun fetchNotices(context: Context) {
        try {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val isLastSeenUpdatable = prefs.getBoolean(PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE)
            val localLastUpdated = prefs.getNullable<LocalDateTime>(PreferenceKey.NOTICES_LAST_SEEN)
            val now = LocalDateTime.now()

            SatenaApplication.instance.accountLoader.signInHatenaAsync().await()

            val response = HatenaClient.getNoticesAsync().await()
            if (isLastSeenUpdatable) {
                try {
                    HatenaClient.updateNoticesLastSeenAsync().start()
                }
                catch (e: Throwable) {
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
                    invokeNotice(context, notice, prefs)
                    Log.d("Notification", "new notice: ${notice.verb}")
                }
            }
        }
        catch (e: Throwable) {
            Log.e("failedToFetchNotices", Log.getStackTraceString(e))
        }
    }

    /** 通知チャンネルを準備する */
    private fun initializeChannel(notificationManager: NotificationManagerCompat, channelName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // グループ生成
        notificationManager.createNotificationChannelGroups(
            listOf(
                NotificationChannelGroup(NOTICE_CHANNEL_ID, channelName)
            )
        )

        // チャンネル生成
        if (notificationManager.getNotificationChannel(NOTICE_CHANNEL_ID) != null) {
            notificationManager.deleteNotificationChannel(NOTICE_CHANNEL_ID)
        }
        val noticeChannel = NotificationChannel(
            NOTICE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = NOTICE_CHANNEL_ID
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            enableLights(true)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(noticeChannel)
    }

    /** 通知を表示する */
    private suspend fun invokeNotice(
        context: Context,
        notice: Notice,
        prefs: SafeSharedPreferences<PreferenceKey>
    ) {
        if (checkFromSpam(prefs, notice)) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        val channelName = context.getString(R.string.notification_channel_name)
        initializeChannel(notificationManager, channelName)

        val title = context.getString(R.string.notice_title)
        val message = makeSpannedFromHtml(notice.message(context)).toString()

        var actions : List<NotificationCompat.Action>? = null
        val intent = when (notice.verb) {
            Notice.VERB_STAR -> {
                runCatching {
                    val openEntryIntent = Intent(context, BookmarksActivity::class.java).apply {
                        putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
                    }

                    val openBookmarkIntent = Intent(context, BookmarksActivity::class.java).apply {
                        putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
                        putExtra(BookmarksActivity.EXTRA_TARGET_USER, HatenaClient.account!!.name)
                    }

                    val openNoticesIntent = Intent(context, EntriesActivity::class.java).apply {
                        putExtra(EntriesActivity.EXTRA_OPEN_NOTICES, true)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    actions = listOf(
                        NotificationCompat.Action(
                            0,
                            context.getString(R.string.notice_action_open_entry),
                            PendingIntent.getActivity(
                                context,
                                1,
                                openEntryIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        ),
                        NotificationCompat.Action(
                            0,
                            context.getString(R.string.notice_action_open_bookmark),
                            PendingIntent.getActivity(
                                context,
                                2,
                                openBookmarkIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        ),
                        NotificationCompat.Action(
                            0,
                            context.getString(R.string.notice_action_open_notices),
                            PendingIntent.getActivity(
                                context,
                                3,
                                openNoticesIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    )

                    // デフォルトで開くのはブコメ詳細
                    openBookmarkIntent
                }.getOrElse {
                    actions = null
                    Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
                }
            }

            Notice.VERB_ADD_FAVORITE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
            }

            else -> {
                Intent(context, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_OPEN_NOTICES, true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }

        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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

        notificationManager.notify(0, builder.build())
    }

    /**
     * 通知するべきかをチェックする
     *
     * @return true: スパムと思われるので通知しない
     */
    private suspend fun checkFromSpam(
        prefs: SafeSharedPreferences<PreferenceKey>,
        notice: Notice
    ) : Boolean {
        try {
            val ignoreNoticesFromSpam = prefs.getBoolean(PreferenceKey.IGNORE_NOTICES_FROM_SPAM)
            if (!ignoreNoticesFromSpam) return false

            if (notice.checkFromSpam()) return true
            if (notice.verb != Notice.VERB_STAR) return false

            // ブコメをつけていないのにスターがつけられた
            val bookmarkPage = HatenaClient.getBookmarkPageAsync(notice.eid, notice.user).await()
            if (bookmarkPage.comment.body.isBlank()) return true

            // スターがすぐに取り消された
            val user = notice.objects.firstOrNull()?.user ?: return true
            val starsEntry = HatenaClient.getStarsEntryAsync(notice.link).await()
            if (starsEntry.allStars.none { it.user == user }) return true
        }
        catch (e: Throwable) {
            Log.e("checkFromSpam", Log.getStackTraceString(e))
        }

        return false
    }
}
