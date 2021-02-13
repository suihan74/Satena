package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.view.Gravity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.models.AppUpdateNoticeMode
import com.suihan74.satena.models.DialogThemeSetting
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.putObjectExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 「基本」画面
 */
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
    val drawerGravity = createLiveData<Int>(
        PreferenceKey.DRAWER_GRAVITY
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

    /** 画像キャッシュサイズ */
    val imageCacheSize : LiveData<Long> by lazy { _imageCacheSize }
    private val _imageCacheSize = MutableLiveData<Long>()

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(activity: PreferencesActivity, fragmentManager: FragmentManager) = buildList {
        addSection(R.string.pref_generals_section_theme)
        addPrefItem(theme, R.string.pref_generals_theme_desc) { openAppThemeSelectionDialog(fragmentManager) }
        addPrefItem(dialogTheme, R.string.pref_generals_dialog_theme_desc) { openDialogThemeSelectionDialog(fragmentManager) }
        addPrefItem(drawerGravity, R.string.pref_generals_drawer_gravity_desc) { openDrawerGravitySelectionDialog(fragmentManager) }

        // --- //

        addSection(R.string.pref_generals_section_update)
        addPrefItem(appUpdateNoticeMode, R.string.pref_generals_app_update_notice_mode_desc) {
            openAppUpdateNoticeModeSelectionDialog(fragmentManager)
        }
        addPrefToggleItem(noticeIgnoredAppUpdate, R.string.pref_generals_notice_ignored_app_update_desc)

        // --- //

        addSection(R.string.pref_generals_section_notices)
        addPrefToggleItem(checkNotices, R.string.pref_generals_background_checking_notices_desc)
        addPrefItem(checkNoticesInterval, R.string.pref_generals_checking_notices_intervals_desc, R.string.minutes) {
            openCheckingNoticesIntervalSelectionDialog(fragmentManager)
        }
        addPrefToggleItem(noticesLastSeenUpdatable, R.string.pref_generals_notices_last_seen_updatable_desc)
        addPrefToggleItem(ignoreNoticesToSilentBookmark, R.string.pref_generals_ignore_notices_from_spam_desc)

        // --- //

        addSection(R.string.pref_generals_section_dialogs)
        addPrefToggleItem(closeDialogOnTouchOutside, R.string.pref_generals_close_dialog_touch_outside_desc)
        addPrefToggleItem(confirmTerminationDialog, R.string.pref_generals_using_termination_dialog_desc)
        addPrefToggleItem(displayReleaseNotes, R.string.pref_generals_show_release_notes_after_update_desc)

        // --- //

        addSection(R.string.pref_generals_section_backup)
        add(PreferencesAdapter.Button(R.string.pref_information_save_settings_desc) {
            activity.openSaveSettingsDialog()
        })
        add(PreferencesAdapter.Button(R.string.pref_information_load_settings_desc) {
            activity.openLoadSettingsDialog()
        })
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
                GlobalScope.launch(Dispatchers.Main) {
                    val app = SatenaApplication.instance
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

    /** アプリのテーマを選択するダイアログを開く */
    private fun openAppThemeSelectionDialog(fragmentManager: FragmentManager) {
        val values = Theme.values()
        val labelIds = values.map { it.textId }
        val checkedItem = values.indexOf(theme.value)

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_generals_theme_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(
                labelIds,
                checkedItem
            ) { f, which ->
                theme.value = Theme.fromId(which)

                if (checkedItem != which) {
                    restartActivity(f.requireContext())
                }
            }
            .dismissOnClickItem(true)
            .create()
        dialog.show(fragmentManager, null)
    }

    /** ダイアログのテーマを選択するダイアログを開く */
    private fun openDialogThemeSelectionDialog(fragmentManager: FragmentManager) {
        val labelIds = DialogThemeSetting.values().map { it.textId }
        val checkedItem = DialogThemeSetting.values().indexOf(dialogTheme.value)

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_generals_dialog_theme_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(
                labelIds,
                checkedItem
            ) { _, which ->
                dialogTheme.value = DialogThemeSetting.fromOrdinal(which)
            }
            .dismissOnClickItem(true)
            .create()
        dialog.show(fragmentManager, null)
    }

    /** 再起動してテーマ変更を適用する */
    private fun restartActivity(context: Context) {
        val intent = Intent(context, PreferencesActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION

            putObjectExtra(
                PreferencesActivity.EXTRA_CURRENT_TAB,
                PreferencesTabMode.GENERALS
            )
            putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
        }
        context.startActivity(intent)
    }

    /** ドロワの配置を選択するダイアログを開く */
    private fun openDrawerGravitySelectionDialog(fragmentManager: FragmentManager) {
        val items = listOf(Gravity.LEFT, Gravity.RIGHT)
        val labelIds = listOf(R.string.pref_generals_drawer_gravity_left, R.string.pref_generals_drawer_gravity_right)
        val checkedItem = items.indexOf(drawerGravity.value)

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_generals_drawer_gravity_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(
                labelIds,
                checkedItem
            ) { _, which ->
                drawerGravity.value = items[which]
            }
            .dismissOnClickItem(true)
            .create()
        dialog.show(fragmentManager,null)
    }

    /** 新バージョン通知対象を選択するダイアログを開く */
    private fun openAppUpdateNoticeModeSelectionDialog(fragmentManager: FragmentManager) {
        val currentValue = appUpdateNoticeMode.value ?: AppUpdateNoticeMode.FIX
        val currentIdx = AppUpdateNoticeMode.values().indexOf(currentValue)
        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_generals_app_update_notice_mode_desc)
            .setSingleChoiceItems(
                AppUpdateNoticeMode.values().map { it.textId },
                currentIdx
            ) { _, which ->
                appUpdateNoticeMode.value = AppUpdateNoticeMode.fromOrdinal(which)
            }
            .dismissOnClickItem(true)
            .setNegativeButton(R.string.dialog_cancel)
            .create()
        dialog.show(fragmentManager, null)
    }

    /** 通知確認間隔を設定するダイアログを開く */
    private fun openCheckingNoticesIntervalSelectionDialog(fragmentManager: FragmentManager) {
        val dialog = NumberPickerDialog.createInstance(
            min = 15,
            max = 180,
            default = checkNoticesInterval.value!!.toInt(),
            titleId = R.string.pref_generals_checking_notices_intervals_desc,
            messageId = R.string.pref_generals_notices_intervals_dialog_msg
        ) { value ->
            checkNoticesInterval.value = value.toLong()
            SatenaApplication.instance.startCheckingNotificationsWorker(
                SatenaApplication.instance,
                forceReplace = true
            )
        }
        dialog.show(fragmentManager, null)
    }
}
