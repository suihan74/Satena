package com.suihan74.satena.scenes.preferences.pages

import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.AppUpdateNoticeMode
import com.suihan74.satena.models.DialogThemeSetting
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showAllowingStateLoss

class PreferencesGeneralsViewModel(
    prefs: SafeSharedPreferences<PreferenceKey>
) : PreferencesViewModel<PreferenceKey>(prefs) {

    /** アプリのテーマ(ダークテーマか否か) */
    val darkTheme = createLiveData<Boolean>(
        PreferenceKey.DARK_THEME
    )

    /** ダイアログのテーマ */
    val dialogTheme = createLiveDataEnum(
        PreferenceKey.DIALOG_THEME,
        { it.id },
        { i -> DialogThemeSetting.fromId(i) }
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

    // ------ //

    /** ダイアログのテーマを選択するダイアログを開く */
    fun openDialogThemeSelectionDialog(fragmentManager: FragmentManager) {
        val labelIds = DialogThemeSetting.values().map { it.titleId }
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
        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 新バージョン通知対象を選択するダイアログを開く */
    fun openAppUpdateNoticeModeSelectionDialog(fragmentManager: FragmentManager) {
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
        dialog.showAllowingStateLoss(fragmentManager)
    }
}
