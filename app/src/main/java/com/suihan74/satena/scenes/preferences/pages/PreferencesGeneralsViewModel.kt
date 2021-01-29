package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.AppUpdateNoticeMode
import com.suihan74.satena.models.DialogThemeSetting
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreferencesGeneralsViewModel(
    context: Context,
    prefs: SafeSharedPreferences<PreferenceKey>
) : PreferencesViewModel<PreferenceKey>(prefs) {

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
    fun openClearImageCacheConfirmDialog(fragmentManager: FragmentManager) {
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
        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** アプリのテーマを選択するダイアログを開く */
    fun openAppThemeSelectionDialog(fragmentManager: FragmentManager) {
        val values = Theme.values()
        val labelIds = values.map { it.textId }
        val checkedItem = values.indexOf(theme.value)

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_generals_theme_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(
                labelIds,
                checkedItem
            ) { _, which ->
                theme.value = Theme.fromId(which)
            }
            .dismissOnClickItem(true)
            .create()
        dialog.showAllowingStateLoss(fragmentManager)
    }

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
