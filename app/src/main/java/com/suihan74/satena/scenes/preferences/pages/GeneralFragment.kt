package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.models.browser.ClearingImageCacheSpan
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.extensions.requireActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

/**
 * 「基本」画面
 */
class GeneralFragment : ListPreferencesFragment() {
    override val viewModel
        get() = requireActivity<PreferencesActivity>().generalViewModel
}

// ------ //

class GeneralViewModel(context: Context) : ListPreferencesViewModel(context) {
    /** アプリのテーマ */
    val theme = createLiveDataEnum(
        PreferenceKey.THEME,
        { it.id },
        { i -> Theme.fromId(i) }
    )

    /** ダイアログのテーマ */
    val dialogTheme = createLiveDataEnum(
        PreferenceKey.DIALOG_THEME,
        { it.id },
        { i -> DialogThemeSetting.fromId(i) }
    )

    /** ダイアログの外側をタッチしたら閉じる */
    val closeDialogOnTouchOutside = createLiveData<Boolean>(
        PreferenceKey.CLOSE_DIALOG_ON_TOUCH_OUTSIDE
    )

    /** ドロワーの位置 */
    val drawerGravity = createLiveDataEnum(
        PreferenceKey.DRAWER_GRAVITY,
        { it.gravity },
        { i -> GravitySetting.fromGravity(i) }
    )

    /** アプリ内アップデート通知を使用する */
    val appUpdateNoticeMode = createLiveDataEnum(
        PreferenceKey.APP_UPDATE_NOTICE_MODE,
        { m -> m.id },
        { i -> AppUpdateNoticeMode.fromId(i) }
    )

    /** 一度無視したアップデートを再度通知する */
    val noticeIgnoredAppUpdate = createLiveData<Boolean>(
        PreferenceKey.NOTICE_IGNORED_APP_UPDATE
    )

    /** 終了確認ダイアログを表示する */
    val confirmTerminationDialog = createLiveData<Boolean>(
        PreferenceKey.USING_TERMINATION_DIALOG
    )

    /** 通知の既読状態をアプリから更新する */
    val noticesLastSeenUpdatable = createLiveData<Boolean>(
        PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE
    )

    /** 常駐して通知を確認する */
    val checkNotices = createLiveData<Boolean>(
        PreferenceKey.BACKGROUND_CHECKING_NOTICES
    )

    /** 通知を確認する間隔 */
    val checkNoticesInterval = createLiveData<Long>(
        PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
    )

    /** スパムと思われるスター通知を行わない */
    val ignoreNoticesToSilentBookmark = createLiveData<Boolean>(
        PreferenceKey.IGNORE_NOTICES_FROM_SPAM
    )

    /** アップデート後初回起動時にリリースノートを表示する */
    val displayReleaseNotes = createLiveData<Boolean>(
        PreferenceKey.SHOW_RELEASE_NOTES_AFTER_UPDATE
    )

    /** インテント発行時にデフォルトアプリを優先使用する */
    private val useIntentChooser = createLiveData<Boolean>(
        PreferenceKey.USE_INTENT_CHOOSER
    )

    /** 画像キャッシュを消去する間隔 */
    private val clearingImageCacheSpan = createLiveDataEnum(
        PreferenceKey.CLEARING_IMAGE_CACHE_SPAN,
        { it.days },
        { ClearingImageCacheSpan.fromDays(it) }
    )

    /** 画像キャッシュサイズ */
    val imageCacheSize : LiveData<Long> by lazy { _imageCacheSize }
    private val _imageCacheSize = MutableLiveData<Long>()

    private val imageCacheSizeLabel = MutableLiveData<CharSequence>()

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        super.onCreateView(fragment)

        // 通知確認タスクの有効無効を切り替える
        checkNotices.observe(fragment.viewLifecycleOwner) {
            val app = SatenaApplication.instance
            if (it) app.startCheckingNotificationsWorker(app, forceReplace = true)
            else app.stopCheckingNotificationsWorker(app)
        }

        // 通知確認間隔の変更を反映させるためにWorkを再起動する
        checkNoticesInterval.observe(fragment.viewLifecycleOwner) {
            val app = SatenaApplication.instance
            app.startCheckingNotificationsWorker(app, forceReplace = true)
        }

        imageCacheSize.observe(fragment.viewLifecycleOwner) {
            imageCacheSizeLabel.value = createSizeText(it, "B")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(
        context: Context,
        fragmentManager: FragmentManager
    ) = buildList {
        addSection(R.string.pref_generals_section_theme)
        addPrefItem(theme, R.string.pref_generals_theme_desc) {
            openEnumSelectionDialog(
                Theme.values(),
                theme,
                R.string.pref_generals_theme_desc,
                fragmentManager
            ) { f, old, new ->
                if (old != new) {
                    restartActivity(f.requireContext())
                }
            }
        }
        addPrefItem(dialogTheme, R.string.pref_generals_dialog_theme_desc) {
            openEnumSelectionDialog(
                DialogThemeSetting.values(),
                dialogTheme,
                R.string.pref_generals_dialog_theme_desc,
                fragmentManager
            )
        }
        addPrefItem(drawerGravity, R.string.pref_generals_drawer_gravity_desc) {
            openEnumSelectionDialog(
                GravitySetting.values(),
                drawerGravity,
                R.string.pref_generals_drawer_gravity_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_generals_section_update)
        addPrefItem(appUpdateNoticeMode, R.string.pref_generals_app_update_notice_mode_desc) {
            openEnumSelectionDialog(
                AppUpdateNoticeMode.values(),
                appUpdateNoticeMode,
                R.string.pref_generals_app_update_notice_mode_desc,
                fragmentManager
            )
        }
        addPrefToggleItem(noticeIgnoredAppUpdate, R.string.pref_generals_notice_ignored_app_update_desc)

        // --- //

        addSection(R.string.pref_generals_section_notices)
        addPrefToggleItem(checkNotices, R.string.pref_generals_background_checking_notices_desc)
        addPrefItem(checkNoticesInterval, R.string.pref_generals_checking_notices_intervals_desc, R.string.minutes) {
            openNumberPickerDialog(
                checkNoticesInterval,
                min = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS_LOWER_BOUND,
                max = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS_UPPER_BOUND,
                titleId = R.string.pref_generals_checking_notices_intervals_desc,
                messageId = R.string.pref_generals_notices_intervals_dialog_msg,
                fragmentManager = fragmentManager
            ) {
                SatenaApplication.instance.startCheckingNotificationsWorker(
                    SatenaApplication.instance,
                    forceReplace = true
                )
            }
        }
        addPrefToggleItem(noticesLastSeenUpdatable, R.string.pref_generals_notices_last_seen_updatable_desc)
        addPrefToggleItem(ignoreNoticesToSilentBookmark, R.string.pref_generals_ignore_notices_from_spam_desc)

        // --- //

        addSection(R.string.pref_generals_section_intent)
        addPrefToggleItem(useIntentChooser, R.string.pref_generals_use_intent_chooser)

        // --- //

        addSection(R.string.pref_generals_section_dialogs)
        addPrefToggleItem(closeDialogOnTouchOutside, R.string.pref_generals_close_dialog_touch_outside_desc)
        addPrefToggleItem(confirmTerminationDialog, R.string.pref_generals_using_termination_dialog_desc)
        addPrefToggleItem(displayReleaseNotes, R.string.pref_generals_show_release_notes_after_update_desc)

        // --- //

        addSection(R.string.pref_generals_section_backup)
        addButton(context, R.string.pref_generals_save_settings_desc) {
            context.alsoAs<PreferencesActivity> {
                it.openSaveSettingsDialog()
            }
        }
        addButton(context, R.string.pref_generals_load_settings_desc) {
            context.alsoAs<PreferencesActivity> {
                it.openLoadSettingsDialog()
            }
        }

        // --- //

        addSection(R.string.pref_generals_section_clear_caches)
        addPrefItem(clearingImageCacheSpan, R.string.pref_generals_clear_image_cache_span) {
            openEnumSelectionDialog(
                ClearingImageCacheSpan.values(),
                clearingImageCacheSpan,
                R.string.pref_generals_clear_image_cache_span,
                fragmentManager
            )
        }
        addButton(
            context = context,
            text = MutableLiveData(context.getText(R.string.pref_generals_clear_image_cache_desc)),
            subText = imageCacheSizeLabel,
            textColorId = R.color.clearCache
        ) {
            openClearImageCacheConfirmDialog(fragmentManager)
        }
    }

    // ------ //

    init {
        viewModelScope.launch {
            calcImageCacheSize(context)
        }
    }

    // ------ //

    /** 画像キャッシュの合計サイズを計算する */
    private suspend fun calcImageCacheSize(context: Context) = withContext(Dispatchers.IO) {
        val dir = GlideApp.getPhotoCacheDir(context)
        val size = dir?.listFiles()?.sumOf { it.length() } ?: 0
        _imageCacheSize.postValue(size)
    }

    /** 画像キャッシュを削除するか確認するダイアログを開く */
    private fun openClearImageCacheConfirmDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_generals_clear_image_cache_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                val app = SatenaApplication.instance
                app.coroutineScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        GlideApp.get(app).clearDiskCache()
                    }
                    app.showToast(R.string.msg_pref_generals_clear_image_cache_succeeded)
                    calcImageCacheSize(app)
                }
            }
            .create()
        dialog.show(fragmentManager, null)
    }

    // ------ //

    /** 再起動してテーマ変更を適用する */
    private fun restartActivity(context: Context) {
        val intent = Intent(context, PreferencesActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION

            putObjectExtra(
                PreferencesActivity.EXTRA_CURRENT_TAB,
                PreferencesTab.GENERALS
            )
            putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
        }
        context.startActivity(intent)
    }

    // ------ //

    /**
     * 数値を人間が読むのに適した形に編集したテキストに変換する
     */
    private fun createSizeText(size: Long?, unit: String? = "") : String {
        if (size == null) {
            return ""
        }

        val rawSize = kotlin.math.max(0L, size)

        val metrics = arrayOf("", "Ki", "Mi", "Gi", "Ti")
        val exp = kotlin.math.min(
            if (rawSize == 0L) 0
            else floor(log(rawSize.toDouble(), 1024.0)).toInt(),
            metrics.lastIndex
        )
        val metric = metrics[exp]
        val num = rawSize / 1024.0.pow(exp)

        return String.format("%.1f%s%s", num, metric, unit.orEmpty())
    }
}
