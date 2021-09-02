package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.annotation.StringRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemPrefsPostBookmarkAccountStatesBinding
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.TapTitleBarAction
import com.suihan74.satena.scenes.post.TagsListOrder
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.observerForOnlyUpdates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 「ブックマーク」画面
 */
class BookmarkFragment : ListPreferencesFragment() {
    override val viewModel by lazy {
        BookmarkViewModel(
            requireContext(),
            SatenaApplication.instance.accountLoader
        )
    }
}

// ------ //

class BookmarkViewModel(
    context: Context,
    private val accountLoader: AccountLoader
) : ListPreferencesViewModel(context) {
    /** 最初に表示するタブのindex */
    private val initialTabPosition = createLiveDataEnum(
        PreferenceKey.BOOKMARKS_INITIAL_TAB,
        { it.ordinal },
        { BookmarksTabType.fromOrdinal(it) }
    )

    /** ブクマ投稿前に確認ダイアログを表示する */
    private val confirmPostBookmark = createLiveData<Boolean>(
        PreferenceKey.USING_POST_BOOKMARK_DIALOG
    )

    /** スター投稿前に確認ダイアログを表示する */
    private val confirmPostStar = createLiveData<Boolean>(
        PreferenceKey.USING_POST_STAR_DIALOG
    )

    /** ユーザー非表示前に確認ダイアログを表示する */
    private val confirmIgnoreUser = createLiveData<Boolean>(
        PreferenceKey.USING_IGNORE_USER_DIALOG
    )

    /** ブクマ一覧画面の項目に対してスターを付けられるようにする */
    private val useAddStarPopupMenu = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_USE_ADD_STAR_POPUP_MENU
    )

    /** スター付与ボタンのタップ判定領域をブクマ項目右端部分に拡大する */
    private val useAddStarEdge = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_USE_ADD_STAR_EDGE
    )

    /** スクロールでツールバーの表示状態を変化させる */
    private val toggleToolbarByScrolling = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING
    )

    /** スクロールでボタンの表示状態を変化させる */
    private val toggleButtonsByScrolling = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING
    )

    /** タブのスワイプ感度 */
    private val pagerScrollSensitivity = createLiveData<Float>(
        PreferenceKey.BOOKMARKS_PAGER_SCROLL_SENSITIVITY
    )

    /** エクストラスクロール機能のツマミの配置 */
    private val extraScrollingAlignment = createLiveDataEnum(
        PreferenceKey.BOOKMARKS_EXTRA_SCROLL_ALIGNMENT,
        { it.id },
        { ExtraScrollingAlignment.fromId(it) }
    )

    /** 「すべて」タブでは非表示ブクマを表示する */
    private val displayMutedBookmarksInAllBookmarksTab = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS
    )

    /** IDコールの言及先の非表示ブクマを表示する */
    private val displayMutedBookmarksInMention = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING
    )

    /** 非表示ユーザーのスターを表示する */
    private val displayIgnoredUsersStar = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS
    )

    /** リンク部分をタップしたときの動作 */
    private val linkSingleTapAction = createLiveDataEnum(
        PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** リンク部分をロングタップしたときの動作 */
    private val linkLongTapAction = createLiveDataEnum(
        PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** タブ長押しで初期タブを変更する */
    private val changeHomeByLongTapping = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB
    )

    /** タイトルバーをタップしたときの動作 */
    private val titleSingleClickBehavior = createLiveDataEnum(
        PreferenceKey.BOOKMARKS_TITLE_SINGLE_CLICK_BEHAVIOR,
        { it.id },
        { TapTitleBarAction.fromId(it) }
    )

    /** タイトルバーをロングタップしたときの動作 */
    private val titleLongClickBehavior = createLiveDataEnum(
        PreferenceKey.BOOKMARKS_TITLE_LONG_CLICK_BEHAVIOR,
        { it.id },
        { TapTitleBarAction.fromId(it) }
    )

    /** 投稿時のSNS連携状態を引き継ぐ */
    private val saveAccountStates = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_SAVE_STATES
    )

    /** プライベート投稿するかのデフォルト設定 */
    val defaultPrivatePost = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_PRIVATE_DEFAULT_CHECKED
    )

    /** Mastodonに連携投稿するかのデフォルト設定 */
    val defaultPostMastodon = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_MASTODON_DEFAULT_CHECKED
    )

    /** Twitterに連携投稿するかのデフォルト設定 */
    val defaultPostTwitter = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_TWITTER_DEFAULT_CHECKED
    )

    /** Facebookに連携投稿するかのデフォルト設定 */
    val defaultPostFacebook = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_FACEBOOK_DEFAULT_CHECKED
    )

    val signedInMastodon = MutableLiveData<Boolean>()

    val signedInTwitter = MutableLiveData<Boolean>()

    val signedInFacebook = MutableLiveData<Boolean>()

    /** はてなアカウントの認証状態 */
    val signedInHatena = MutableLiveData(
        prefs.contains(PreferenceKey.HATENA_RK)
    )

    /** ダイアログの縦位置 */
    private val verticalGravity = createLiveDataEnum(
        PreferenceKey.POST_BOOKMARK_VERTICAL_GRAVITY,
        { it.ordinal },
        { BookmarkPostActivityGravity.fromOrdinal(it) }
    )

    /** タグ入力ダイアログを最初から最大展開する */
    private val expandAddingTagsDialogByDefault = createLiveData<Boolean>(
        PreferenceKey.POST_BOOKMARK_EXPAND_ADDING_TAGS_DIALOG_BY_DEFAULT
    )

    /** タグリストの並び順 */
    private val tagsListOrder = createLiveDataEnum(prefs, PreferenceKey.POST_BOOKMARK_TAGS_LIST_ORDER,
        { v -> v.ordinal },
        { i -> TagsListOrder.fromOrdinal(i) }
    )

    // ------ //
    /** アプリ独自のダイジェスト抽出機能用の設定ファイル */
    private val customDigestPrefs = SafeSharedPreferences.create<CustomDigestSettingsKey>(context)

    /** アプリ独自のダイジェスト抽出機能を使用する */
    private val useCustomDigest = createLiveData<CustomDigestSettingsKey, Boolean>(
        customDigestPrefs,
        CustomDigestSettingsKey.USE_CUSTOM_DIGEST
    )

    /** 集計時に非表示ユーザーのスターを無視する */
    private val customDigestIgnoreStarsByIgnoredUsers = createLiveData<CustomDigestSettingsKey, Boolean>(
        customDigestPrefs,
        CustomDigestSettingsKey.IGNORE_STARS_BY_IGNORED_USERS
    )

    /** 集計時に連打スターを1個だけ数える */
    private val customDigestDeduplicateStars = createLiveData<CustomDigestSettingsKey, Boolean>(
        customDigestPrefs,
        CustomDigestSettingsKey.DEDUPLICATE_STARS
    )

    /** 最大抽出数 */
    private val customDigestMaxNumOfElements = createLiveData<CustomDigestSettingsKey, Int>(
        customDigestPrefs,
        CustomDigestSettingsKey.MAX_NUM_OF_ELEMENTS
    )

    /** 抽出対象になる最小スター数 */
    private val customDigestStarsCountThreshold = createLiveData<CustomDigestSettingsKey, Int>(
        customDigestPrefs,
        CustomDigestSettingsKey.STARS_COUNT_THRESHOLD
    )

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        super.onCreateView(fragment)

        combine(accountLoader.hatenaFlow, accountLoader.mastodonFlow, ::Pair)
            .onEach { (hatena, mastodon) ->
                val previousSignedInHatena = signedInHatena.value
                signedInHatena.value = hatena != null
                signedInMastodon.value = mastodon?.isLocked == false
                signedInTwitter.value = hatena?.isOAuthTwitter ?: false
                signedInFacebook.value = hatena?.isOAuthFaceBook ?: false
                // はてなのアカウントが解除されたら投稿に関するメニューを隠す
                if (previousSignedInHatena != null && previousSignedInHatena != signedInHatena.value) {
                    load(fragment)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            // 連携SNS情報を取得
            runCatching {
                accountLoader.signInAccounts(reSignIn = false)
            }.onFailure {
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    fragment.showToast(R.string.msg_pref_bookmarks_fetching_accounts_failed)
                }
            }
        }

        // ------ //
        // 値の変更によって他の設定項目の表示状態が変わるもの

        saveAccountStates.observe(fragment.viewLifecycleOwner, observerForOnlyUpdates {
            load(fragment)
        })

        useCustomDigest.observe(fragment.viewLifecycleOwner, observerForOnlyUpdates {
            load(fragment)
        })

        useAddStarPopupMenu.observe(fragment.viewLifecycleOwner, observerForOnlyUpdates {
            load(fragment)
        })
    }

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {
        val fragmentManager = fragment.childFragmentManager

        if (signedInHatena.value == true) {
            addSection(R.string.pref_bookmark_section_posting)
            addPrefToggleItem(fragment, confirmPostBookmark, R.string.pref_bookmarks_using_post_dialog_desc)
            addPrefItem(fragment, verticalGravity, R.string.pref_bookmarks_post_dialog_vertical_gravity_desc) {
                openEnumSelectionDialog(
                    BookmarkPostActivityGravity.values(),
                    verticalGravity,
                    R.string.pref_bookmarks_post_dialog_vertical_gravity_desc,
                    fragmentManager
                )
            }
            addPrefToggleItem(fragment, saveAccountStates, R.string.pref_bookmarks_save_states)
            if (saveAccountStates.value == false) {
                add(
                    PrefItemAccountStatesSetter(
                        R.string.pref_bookmarks_default_accounts_states,
                        this@BookmarkViewModel,
                        fragmentManager
                    )
                )
            }
            addPrefItem(fragment, tagsListOrder, R.string.pref_post_bookmarks_tags_list_order_desc) {
                openEnumSelectionDialog(
                    TagsListOrder.values(),
                    tagsListOrder,
                    R.string.pref_post_bookmarks_tags_list_order_desc,
                    fragmentManager
                )
            }
            addPrefToggleItem(fragment, expandAddingTagsDialogByDefault, R.string.pref_post_bookmarks_expand_adding_tags_dialog_by_default_desc)
        }

        // --- //

        addSection(R.string.pref_bookmark_section_tab)
        addPrefItem(fragment, initialTabPosition, R.string.pref_bookmarks_initial_tab_desc) {
            openEnumSelectionDialog(
                BookmarksTabType.values(),
                initialTabPosition,
                R.string.pref_bookmarks_initial_tab_desc,
                fragmentManager
            )
        }
        addPrefToggleItem(fragment, changeHomeByLongTapping, R.string.pref_bookmarks_change_home_by_long_tapping_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_title_bar)
        addPrefItem(fragment, titleSingleClickBehavior, R.string.pref_bookmarks_title_single_click_behavior_desc) {
            openEnumSelectionDialog(
                TapTitleBarAction.values(),
                titleSingleClickBehavior,
                R.string.pref_bookmarks_title_single_click_behavior_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, titleLongClickBehavior, R.string.pref_bookmarks_title_long_click_behavior_desc) {
            openEnumSelectionDialog(
                TapTitleBarAction.values(),
                titleLongClickBehavior,
                R.string.pref_bookmarks_title_long_click_behavior_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_bookmark_section_behavior)
        addPrefToggleItem(fragment, confirmPostStar, R.string.pref_bookmarks_using_post_star_dialog_desc)
        addPrefToggleItem(fragment, useAddStarPopupMenu, R.string.pref_bookmarks_using_add_star_popup_menu_desc)
        if (useAddStarPopupMenu.value == true) {
            addPrefToggleItem(fragment, useAddStarEdge, R.string.pref_bookmarks_using_add_star_edge_desc)
        }
        addPrefToggleItem(fragment, toggleToolbarByScrolling, R.string.pref_bookmarks_hiding_toolbar_by_scrolling)
        addPrefToggleItem(fragment, toggleButtonsByScrolling, R.string.pref_bookmarks_hiding_buttons_with_scrolling_desc)
        addButton(fragment, R.string.pref_pager_scroll_sensitivity_desc) {
            SliderDialog.createInstance(
                titleId = R.string.pref_pager_scroll_sensitivity_desc,
                messageId = R.string.pref_pager_scroll_sensitivity_dialog_message,
                min = 0.1f,
                max = 1f,
                value = pagerScrollSensitivity.value ?: 1f
            ).setOnCompleteListener { value, _ ->
                pagerScrollSensitivity.value = value
            }
            .show(fragmentManager, "")
        }
        addPrefItem(fragment, extraScrollingAlignment, R.string.pref_extra_scroll_align_desc) {
            openEnumSelectionDialog(
                ExtraScrollingAlignment.values(),
                extraScrollingAlignment,
                R.string.pref_extra_scroll_align_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_bookmark_section_ignoring)
        addPrefToggleItem(fragment, confirmIgnoreUser, R.string.pref_bookmarks_using_ignore_user_dialog_desc)
        addPrefToggleItem(fragment, displayMutedBookmarksInAllBookmarksTab, R.string.pref_bookmarks_showing_ignored_users_in_all_bookmarks_desc)
        addPrefToggleItem(fragment, displayMutedBookmarksInMention, R.string.pref_bookmarks_showing_ignored_users_with_calling_desc)
        addPrefToggleItem(fragment, displayIgnoredUsersStar, R.string.pref_bookmarks_showing_stars_of_ignored_users_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_link)
        addPrefItem(fragment, linkSingleTapAction, R.string.pref_bookmark_link_single_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                linkSingleTapAction,
                R.string.pref_bookmark_link_single_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, linkLongTapAction, R.string.pref_bookmark_link_long_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                linkLongTapAction,
                R.string.pref_bookmark_link_long_tap_action_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_bookmark_section_custom_digest)
        addPrefToggleItem(fragment, useCustomDigest, R.string.digest_bookmarks_use_custom_digest_desc)
        if (useCustomDigest.value == true) {
            addPrefToggleItem(fragment, customDigestIgnoreStarsByIgnoredUsers, R.string.digest_bookmarks_exclude_ignored_users_desc)
            addPrefToggleItem(fragment, customDigestDeduplicateStars, R.string.digest_bookmarks_deduplicate_stars_desc)
            addPrefItem(fragment, customDigestMaxNumOfElements, R.string.digest_bookmarks_max_num_of_elements_picker_title) {
                openNumberPickerDialog(
                    customDigestMaxNumOfElements,
                    min = CustomDigestSettingsKey.MAX_NUM_OF_ELEMENTS_LOWER_BOUND,
                    max = CustomDigestSettingsKey.MAX_NUM_OF_ELEMENTS_UPPER_BOUND,
                    titleId = R.string.digest_bookmarks_max_num_of_elements_picker_title,
                    messageId = null,
                    fragmentManager = fragmentManager
                )
            }
            addPrefItem(fragment, customDigestStarsCountThreshold, R.string.digest_bookmarks_stars_count_threshold_picker_title) {
                openNumberPickerDialog(
                    customDigestStarsCountThreshold,
                    min = CustomDigestSettingsKey.STARS_COUNT_THRESHOLD_LOWER_BOUND,
                    max = CustomDigestSettingsKey.STARS_COUNT_THRESHOLD_UPPER_BOUND,
                    titleId = R.string.digest_bookmarks_stars_count_threshold_picker_title,
                    messageId = null,
                    fragmentManager = fragmentManager
                )
            }
        }
    }

    // ------ //

    /**
     * 連携アカウントのデフォルト選択状態を編集する
     */
    class PrefItemAccountStatesSetter(
        @StringRes private val titleId : Int,
        private val viewModel : BookmarkViewModel,
        private val fragmentManager: FragmentManager
    ) : PreferencesAdapter.Item {
        override val layoutId: Int
            get() = R.layout.listview_item_prefs_post_bookmark_account_states

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsPostBookmarkAccountStatesBinding> {
                it.vm = viewModel
                it.titleId = titleId
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemAccountStatesSetter && new is PrefItemAccountStatesSetter &&
                    old.titleId == new.titleId

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemAccountStatesSetter && new is PrefItemAccountStatesSetter &&
                    old.fragmentManager == new.fragmentManager &&
                    old.titleId == new.titleId &&
                    old.viewModel == new.viewModel
    }
}
