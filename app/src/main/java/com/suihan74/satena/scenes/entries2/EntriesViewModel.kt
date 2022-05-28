package com.suihan74.satena.scenes.entries2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.ExtraScrollingAlignment
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.entries2.dialog.BrowserShortcutDialog
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.topBackStackEntry

class EntriesViewModel(
    val repository : EntriesRepository
) : ViewModel() {
    /** カテゴリリスト */
    val categories = repository.categoriesLiveData

    /** サインイン状態 */
    val signedIn = repository.signedInLiveData

    /** ツールバーに表示するタイトル */
    val toolbarTitle = MutableLiveData<String>("")

    /** ツールバーに表示するサブタイトル */
    val toolbarSubTitle = MutableLiveData<String>("")

    /** サインイン/マイブックマークボタンの説明テキスト */
    val myBookmarkButtonTextId = MutableLiveData<Int>().also { resId ->
        signedIn.observeForever {
            resId.value =
                if (it) R.string.my_bookmarks_button_desc_my_bookmarks
                else R.string.my_bookmarks_button_desc_sign_in
        }
    }

    /** サインイン/マイブックマークボタンのアイコン */
    val myBookmarkButtonIconId = MutableLiveData<Int>().also { resId ->
        signedIn.observeForever {
             resId.value =
                 if (it) R.drawable.ic_mybookmarks
                 else R.drawable.ic_baseline_person_add
        }
    }

    /** ドロワ位置 */
    val drawerGravity
        get() = repository.drawerGravity

    /** ホームカテゴリ */
    val homeCategory : Category
        get() = repository.homeCategory

    /** FABメニューにタップ防止背景を表示する */
    val isFABMenuBackgroundActive : Boolean
        get() = repository.isFABMenuBackgroundActive

    /** スクロールにあわせてツールバーを隠す */
    val hideToolbarByScroll : Boolean
        get() = repository.hideToolbarByScroll

    /** エントリ項目クリック時の挙動 */
    val entryClickedAction : TapEntryAction
        get() = repository.entryClickedAction

    /** エントリ項目複数回クリック時の挙動 */
    val entryMultipleClickedAction : TapEntryAction
        get() = repository.entryMultipleClickedAction

    /** エントリ項目長押し時の挙動 */
    val entryLongClickedAction : TapEntryAction
        get() = repository.entryLongClickedAction

    /** エントリ項目のクリック回数判定時間 */
    val entryMultipleClickDuration: Long
        get() = repository.entryMultipleClickDuration

    /** アプリ終了前に確認する */
    val isTerminationDialogEnabled : Boolean
        get() = repository.isTerminationDialogEnabled

    /** ボタン類を画面下部に集約する */
    val isBottomLayoutMode : Boolean
        get() = repository.isBottomLayoutMode

    /** スクロールにあわせて下部バーを隠す */
    val hideBottomAppBarByScroll : Boolean
        get() = repository.hideBottomAppBarByScroll

    /** タブページャのスワイプ感度 */
    val pagerScrollSensitivity : Float
        get() = repository.pagerScrollSensitivity

    /** カテゴリリストの表示形式 */
    val categoriesMode : CategoriesMode
        get() = repository.categoriesMode

    /** ボトムバーの項目 */
    val bottomBarItems : List<UserBottomItem>
        get() = repository.bottomBarItems.mapNotNull { item ->
            if (item.requireSignedIn && signedIn.value != true) null
            else item
        }

    /** ボトムバーの項目の配置方法 */
    val bottomBarItemsGravity : Int
        get() = repository.bottomBarItemsGravity

    /** ボトムバーの追加項目の配置方法 */
    val extraBottomItemsAlignment : ExtraBottomItemsAlignment
        get() = repository.extraBottomItemsAlignment

    /** エクストラボトムメニューを使用する */
    val useExtraBottomMenu : Boolean
        get() = repository.useExtraBottomMenu

    /** 表示中画面のタブ位置 */
    val currentTabPosition = MutableLiveData<Int>()

    val extraScrollingAlignment
        get() = repository.extraScrollingAlignment

    val extraScrollBarVisibility =
        MutableLiveData(extraScrollingAlignment != ExtraScrollingAlignment.NONE)

    fun updateExtraScrollBarVisibility(visibility: Boolean) {
        extraScrollBarVisibility.value = extraScrollingAlignment != ExtraScrollingAlignment.NONE && visibility
    }

    // ------ //

    /** 初期化処理 */
    suspend fun initialize(forceUpdate: Boolean) = repository.initialize(forceUpdate)

    /** アプリアップデートを確認する */
    fun startAppUpdate(activity: EntriesActivity, snackBarAnchorView: View, requestCode: Int) {
        repository.startAppUpdateManager { info ->
            when (info.updateAvailability()) {
                // アップデートを行うかを確認する通知を表示する
                UpdateAvailability.UPDATE_AVAILABLE ->
                    noticeAppUpdate(activity, snackBarAnchorView, info, requestCode)

                // アップデートが中断された場合は再開する
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    resumeAppUpdate(activity, info, requestCode)

                else -> {}
            }
        }
    }

    /** アプリのアップデートを通知する */
    private fun noticeAppUpdate(
        activity: EntriesActivity,
        snackBarAnchorView: View,
        info: AppUpdateInfo,
        requestCode: Int
    ) {
        if (!info.isImmediateUpdateAllowed) return

        val snackBar = Snackbar.make(
            snackBarAnchorView,
            R.string.app_update_notice,
            Snackbar.LENGTH_INDEFINITE
        ).also {
            it.setAction(R.string.app_update_ok) {
                resumeAppUpdate(activity, info, requestCode)
            }

            // 明示的に背景色と文字色を設定する
            it.view.setBackgroundColor(
                activity.getThemeColor(R.attr.snackBarBackgroundColor)
            )
            it.setActionTextColor(
                activity.getThemeColor(R.attr.snackBarActionTextColor)
            )
            it.setTextColor(
                activity.getThemeColor(R.attr.snackBarTextColor)
            )
        }

        snackBar.show()
    }

    /** アプリのアップデートを開始する */
    private fun resumeAppUpdate(activity: EntriesActivity, info: AppUpdateInfo, requestCode: Int) {
        repository.resumeAppUpdate(activity, info, requestCode)
    }

    // ------ //

    /** (基本の)ボトムバーアイテムを選択した際の処理 */
    fun onBasicBottomMenuItemClicked(activity: EntriesActivity, item: UserBottomItem) = when (item) {
        UserBottomItem.SCROLL_TO_TOP -> activity.scrollToTop()

        UserBottomItem.MYBOOKMARKS -> showCategory(activity, Category.MyBookmarks)

        UserBottomItem.NOTICE -> showCategory(activity, Category.Notices)

        UserBottomItem.INNER_BROWSER -> activity.startInnerBrowser()

        UserBottomItem.EXCLUDED_ENTRIES -> activity.openExcludedEntriesDialog()

        UserBottomItem.SEARCH -> showCategory(activity, Category.Search)

        UserBottomItem.HOME -> showCategory(activity, homeCategory)

        UserBottomItem.PREFERENCES -> {
            val intent = Intent(activity, PreferencesActivity::class.java)
            activity.startActivity(intent)
        }

        UserBottomItem.OPEN_OFFICIAL_TOP -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://b.hatena.ne.jp/"))
            activity.startActivity(intent)
        }

        UserBottomItem.OPEN_OFFICIAL_HATENA -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hatena.ne.jp/"))
            activity.startActivity(intent)
        }

        UserBottomItem.OPEN_ANONYMOUS_DIARY -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anond.hatelabo.jp/"))
            activity.startActivity(intent)
        }

        UserBottomItem.CATEGORIES -> activity.openDrawer()
    }

    /**
     * ボトムバーアイテムをロングタップしたときの処理
     *
     * `UserBottomItem#longClickable`が`true`に設定されてるアイテムのみ呼ばれる
     */
    fun onBottomMenuItemLongClicked(
        activity: AppCompatActivity,
        item: UserBottomItem,
        tag: String? = null
    ) = when (item) {
        UserBottomItem.INNER_BROWSER -> {
            val dialog = BrowserShortcutDialog.createInstance()
            dialog.show(activity.supportFragmentManager, tag)
        }

        else -> {}
    }

    /** (カテゴリメニューから遷移できる)カテゴリを選択 */
    fun showCategory(activity: AppCompatActivity, category: Category) {
        showContentFragment(activity, category) {
            category.createFragment()
        }
    }

    /** Category.Siteに遷移 */
    fun showSiteEntries(activity: AppCompatActivity, siteUrl: String) {
        showContentFragment(activity, Category.Site) {
            Category.Site.createSiteFragment(siteUrl)
        }
    }

    /** Category.Userに遷移 */
    fun showUserEntries(activity: AppCompatActivity, user: String) {
        showContentFragment(activity, Category.User) {
            Category.User.createUserFragment(user)
        }
    }

    /** Category.Search */
    fun showSearchEntries(activity: AppCompatActivity, query: String, searchType: SearchType) {
        showContentFragment(activity, Category.Search) {
            Category.Search.createSearchFragment(query, searchType)
        }
    }

    /** EntriesFragment遷移処理 */
    private fun showContentFragment(activity: AppCompatActivity, category: Category, fragmentGenerator: ()->EntriesFragment) {
        // 現在トップにある画面と同じカテゴリには連続して遷移しない
        if (activity.supportFragmentManager.topBackStackEntry?.name == category.name) return

        // 既に一度表示されているカテゴリの場合再利用する
        val existed = activity.supportFragmentManager.findFragmentByTag(category.name)
        val fragment = existed ?: fragmentGenerator()

        activity.supportFragmentManager.beginTransaction().run {
            replace(R.id.main_layout, fragment, category.name)
            addToBackStack(category.name)
            commitAllowingStateLoss()
        }
    }

    // ------ //

    /**
     * ホームカテゴリ・初期表示タブの設定ダイアログを開く
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun openDefaultTabSettingDialog(context: Context, category: Category, tab: EntriesTabType, fragmentManager: FragmentManager) {
        val categoryText = context.getText(category.textId)
        val tabText = context.getText(tab.textId)
        val items = buildList {
            if (category != repository.homeCategory && category.willBeHome && category.displayInList) {
                add(context.getString(R.string.entries_change_home_category, categoryText) to { repository.updateHomeCategory(category) })
            }
            add(context.getString(R.string.entries_change_default_tab, categoryText, tabText) to { repository.updateDefaultTab(category, tab.tabOrdinal) })
        }
        val labels = items.map { it.first }
        val states = BooleanArray(items.size) { true }

        AlertDialogFragment.Builder()
            .setTitle(R.string.entries_change_home_category_tab_title)
            .setMultipleChoiceItems(labels, states) { _, which, state ->
                states[which] = state
            }
            .setPositiveButton(R.string.dialog_register) {
                states.forEachIndexed { index, b ->
                    if (b) {
                        items[index].second.invoke()
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel)
            .create()
            .show(fragmentManager, null)
    }
}
