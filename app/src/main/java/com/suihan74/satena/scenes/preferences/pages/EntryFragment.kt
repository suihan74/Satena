package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.annotation.StringRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemPrefsBottomItemsBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.dialogs.TextInputDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.models.browser.ReadEntryLifetime
import com.suihan74.satena.models.readEntry.ReadEntryBehavior
import com.suihan74.satena.models.readEntry.ReadEntryCondition
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.entries2.ExtraBottomItemsAlignment
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.satena.scenes.post.BookmarkPostRepository
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.satena.scenes.preferences.bottomBar.BottomBarItemSelectionDialog
import com.suihan74.satena.scenes.preferences.bottomBar.UserBottomItemsSetter
import com.suihan74.satena.scenes.preferences.entries.EntriesDefaultTabsFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.and
import com.suihan74.utilities.extensions.observerForOnlyUpdates
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 「エントリ」画面
 */
class EntryFragment : ListPreferencesFragment() {
    override val viewModel by lazy {
        EntryViewModel(requireContext())
    }
}

// ------ //

class EntryViewModel(context: Context) : ListPreferencesViewModel(context) {

    private val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)

    private val readEntriesRepo = SatenaApplication.instance.readEntriesRepository

    /** レイアウトモード */
    val bottomLayoutMode = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_BOTTOM_LAYOUT_MODE
    )

    /** 下部レイアウトをスクロールで隠す */
    private val hideBottomLayoutByScroll = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_HIDE_BOTTOM_LAYOUT_BY_SCROLLING
    )

    /** 下部バーに表示するボタン */
    val bottomBarButtons = createLiveData<List<UserBottomItem>>(
        PreferenceKey.ENTRIES_BOTTOM_ITEMS
    )

    /** 下部バーの項目を右詰めで表示するか左詰めで表示するか */
    val bottomBarButtonsGravity = createLiveDataEnum(
        PreferenceKey.ENTRIES_BOTTOM_ITEMS_GRAVITY,
        { it.gravity },
        { GravitySetting.fromGravity(it) }
    )

    /** 下部バーの追加項目の配置方法 */
    private val extraBottomItemsAlignment = createLiveDataEnum(
        PreferenceKey.ENTRIES_EXTRA_BOTTOM_ITEMS_ALIGNMENT,
        { it.id },
        { ExtraBottomItemsAlignment.fromId(it) }
    )

    /** エクストラスクロール機能のツマミの配置 */
    private val extraScrollingAlignment = createLiveDataEnum(
        PreferenceKey.ENTRIES_EXTRA_SCROLL_ALIGNMENT,
        { it.id },
        { ExtraScrollingAlignment.fromId(it) }
    )

    /** エントリ項目シングルタップの挙動 */
    private val singleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_SINGLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目複数回タップの挙動 */
    private val multipleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目ロングタップの挙動 */
    private val longTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_LONG_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目右端シングルタップの挙動 */
    private val edgeSingleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_EDGE_SINGLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目右端複数回タップの挙動 */
    private val edgeMultipleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_EDGE_MULTIPLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目右端ロングタップの挙動 */
    private val edgeLongTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_EDGE_LONG_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** エントリ項目タップ回数判定時間 */
    private val multipleTapDuration = createLiveData<Long>(
        PreferenceKey.ENTRY_MULTIPLE_TAP_DURATION
    )

    /** 最初に表示するカテゴリ */
    private val homeCategory = createLiveDataEnum<Category>(
        PreferenceKey.ENTRIES_HOME_CATEGORY,
        { it.id },
        { Category.fromId(it) }
    )

    /** カテゴリリストの表示形式 */
    private val categoriesMode = createLiveDataEnum<CategoriesMode>(
        PreferenceKey.ENTRIES_CATEGORIES_MODE
    )

    /** メニュー表示中の操作を許可 */
    private val menuTapGuard = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_MENU_TAP_GUARD
    )

    /** スクロールでツールバーを隠す */
    private val hideToolbarWithScroll = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING
    )

    /** タブ移動のスワイプ感度 */
    private val pagerScrollSensitivity = createLiveData<Float>(
        PreferenceKey.ENTRIES_PAGER_SCROLL_SENSITIVITY
    )

    /** ブクマ閲覧履歴の最大保存数 */
    private val historyMaxSize = createLiveData<EntriesHistoryKey, Int>(
        historyPrefs,
        EntriesHistoryKey.MAX_SIZE
    )

    /** エントリ既読エントリの振舞い */
    private val readEntryBehavior = readEntriesRepo.readEntryBehavior

    /** 既読マークをつける条件 */
    private val readMarkCondition = readEntriesRepo.readEntryCondition

    /** 既読エントリを隠すカテゴリ */
    private val categoriesHidingReadEntries = readEntriesRepo.categoriesHidingReadEntries

    /** 既読情報の寿命 */
    private val readMarkLifetime = createLiveDataEnum(
        PreferenceKey.ENTRY_READ_MARK_LIFETIME,
        { it.days },
        { ReadEntryLifetime.fromDays(it) }
    )

    /** タブ長押しでホームカテゴリ・初期タブを変更する */
    private val changeHomeByLongTappingTab = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB
    )

    /** エントリメニューから「あとで読む」するときにデフォルトで非公開ブクマにする */
    private val privateReadLater = createLiveData<Boolean>(
        PreferenceKey.ENTRY_PRIVATE_READ_LATER
    )

    /** 「あとで読む」エントリを「読んだ」したときの挙動 */
    private val entryReadActionType = createLiveDataEnum<EntryReadActionType>(
        PreferenceKey.ENTRY_READ_ACTION_TYPE
    )

    /** EntryReadActionType.BOILERPLATE時の定型文 */
    private val entryReadActionBoilerPlate = createLiveData<String>(
        PreferenceKey.ENTRY_READ_ACTION_BOILERPLATE
    )

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        super.onCreateView(fragment)

        val owner = fragment.viewLifecycleOwner

        // ボトムバー設定項目の表示を切り替える
        bottomLayoutMode.observe(owner, observerForOnlyUpdates {
            load(fragment)
        })
        // 「読んだ」したときの定型文編集ビューを表示切替え
        entryReadActionType.observe(owner, observerForOnlyUpdates {
            load(fragment)
        })
        // 既読エントリの振舞い
        readEntryBehavior.onEach {
            load(fragment)
        }.launchIn(fragment.lifecycleScope)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment) = buildList {
        val fragmentManager = fragment.childFragmentManager

        addSection(R.string.pref_entry_section_bottom_menu)
        addPrefToggleItem(fragment, bottomLayoutMode, R.string.pref_entries_layout_mode_desc)
        if (bottomLayoutMode.value == true) {
            addPrefToggleItem(fragment, hideBottomLayoutByScroll,R.string.pref_hide_bottom_appbar_by_scrolling_desc)
            add(PrefItemBottomMenuSetter(R.string.pref_bottom_bar_items_desc, this@EntryViewModel, fragmentManager))
            addPrefItem(fragment, bottomBarButtonsGravity, R.string.pref_bottom_menu_items_gravity_desc) {
                openEnumSelectionDialog(
                    GravitySetting.values(),
                    bottomBarButtonsGravity,
                    R.string.pref_bottom_menu_items_gravity_desc,
                    fragmentManager
                )
            }
            addPrefItem(fragment, extraBottomItemsAlignment, R.string.pref_extra_bottom_items_alignment_desc) {
                openEnumSelectionDialog(
                    ExtraBottomItemsAlignment.values(),
                    extraBottomItemsAlignment,
                    R.string.pref_extra_bottom_items_alignment_desc,
                    fragmentManager
                )
            }
        }

        // --- //

        addSection(R.string.pref_entry_section_click)
        addPrefItem(fragment, singleTapAction, R.string.pref_entries_single_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                singleTapAction,
                R.string.pref_entries_single_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, multipleTapAction, R.string.pref_entries_multiple_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                multipleTapAction,
                R.string.pref_entries_multiple_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, longTapAction, R.string.pref_entries_long_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                longTapAction,
                R.string.pref_entries_long_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, edgeSingleTapAction, R.string.pref_entries_edge_single_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                edgeSingleTapAction,
                R.string.pref_entries_edge_single_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, edgeMultipleTapAction, R.string.pref_entries_edge_multiple_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                edgeMultipleTapAction,
                R.string.pref_entries_edge_multiple_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, edgeLongTapAction, R.string.pref_entries_edge_long_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                edgeLongTapAction,
                R.string.pref_entries_edge_long_tap_action_desc,
                fragmentManager
            )
        }

        addPrefItem(fragment, multipleTapDuration, R.string.pref_entries_multiple_tap_duration_desc, R.string.pref_entries_multiple_tap_duration_unit) {
            openMultipleTapDurationDialog(fragmentManager)
        }

        // --- //

        addSection(R.string.pref_entry_section_category)
        addPrefItem(fragment, homeCategory, R.string.home_category_desc) {
            val categories = (
                if (HatenaClient.signedIn()) Category.valuesWithSignedIn()
                else Category.valuesWithoutSignedIn()
            ).filter { it.willBeHome }.toTypedArray()

            openEnumSelectionDialog(
                categories,
                homeCategory,
                R.string.home_category_desc,
                fragmentManager
            )
        }
        addButton(fragment, R.string.pref_entries_default_tabs_desc) {
            fragment.childFragmentManager.beginTransaction()
                .replace(R.id.main_layout, EntriesDefaultTabsFragment.createInstance())
                .commit()
        }
        addPrefToggleItem(fragment, changeHomeByLongTappingTab, R.string.pref_entries_change_home_by_long_tapping_desc)
        addPrefItem(fragment, categoriesMode, R.string.pref_entries_categories_mode_desc) {
            openEnumSelectionDialog(
                CategoriesMode.values(),
                categoriesMode,
                R.string.pref_entries_categories_mode_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_entry_section_behavior)
        addPrefToggleItem(fragment, menuTapGuard, R.string.pref_entries_menu_tap_guard_desc)
        addPrefToggleItem(fragment, hideToolbarWithScroll, R.string.pref_entries_hiding_toolbar_by_scrolling_desc)
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

        addSection(R.string.pref_entry_section_history)
        addPrefItem(fragment, historyMaxSize, R.string.pref_entries_history_max_size_desc, R.string.pref_entries_history_max_size_text) {
            openNumberPickerDialog(
                historyMaxSize,
                min = EntriesHistoryKey.MAX_SIZE_LOWER_BOUND,
                max = EntriesHistoryKey.MAX_SIZE_UPPER_BOUND,
                titleId = R.string.pref_entries_history_max_size_dialog_title,
                messageId = R.string.pref_entries_history_max_size_dialog_msg,
                fragmentManager = fragmentManager
            )
        }

        addPrefItem(fragment, readEntryBehavior, R.string.pref_entries_display_read_mark_desc) {
            openEnumSelectionDialog(
                ReadEntryBehavior.values(),
                readEntryBehavior,
                R.string.pref_entries_display_read_mark_desc,
                fragmentManager
            )
        }
        if (readEntryBehavior.value == ReadEntryBehavior.HIDE_ENTRY) {
            addButton(fragment, R.string.pref_entries_categories_hiding_read_entries_desc) {
                openCategoriesHidingReadEntriesSelectionDialog(fragmentManager)
            }
        }
        if (readEntryBehavior.value != ReadEntryBehavior.NONE) {
            addPrefItem(fragment, readMarkCondition, R.string.pref_entries_read_mark_condition_desc) {
                openReadEntryConditionSelectionDialog(fragmentManager)
            }

            addPrefItem(fragment, readMarkLifetime, R.string.pref_entries_read_mark_lifetime_desc) {
                openEnumSelectionDialog(
                    ReadEntryLifetime.values(),
                    readMarkLifetime,
                    R.string.pref_entries_read_mark_lifetime_desc,
                    fragmentManager
                )
            }
        }

        // --- //

        addSection(R.string.pref_entry_section_read_later)
        addPrefToggleItem(fragment, privateReadLater, R.string.pref_entries_private_read_later_desc)
        addPrefItem(fragment, entryReadActionType, R.string.pref_entries_read_action_type_desc) {
            openEnumSelectionDialog(
                EntryReadActionType.values(),
                entryReadActionType,
                R.string.pref_entries_read_action_type_desc,
                fragmentManager
            )
        }
        if (entryReadActionType.value == EntryReadActionType.BOILERPLATE) {
            addPrefItem(fragment, entryReadActionBoilerPlate, R.string.pref_entries_read_action_boilerplate_desc) {
                openReadActionBoilerPlateEditingDialog(fragmentManager)
            }
        }
    }

    // ------ //

    /**
     * 複数回タップを検知する待ち時間を設定するダイアログを開く
     */
    private fun openMultipleTapDurationDialog(fragmentManager: FragmentManager) {
        val dialog = NumberPickerDialog.createInstance(
            min = 0,
            max = 500,
            default = multipleTapDuration.value!!.toInt(),
            titleId = R.string.pref_entries_multiple_tap_duration_desc,
            messageId = R.string.pref_entries_multiple_tap_duration_dialog_message
        ) { value ->
            multipleTapDuration.value = value.toLong()
        }
        dialog.show(fragmentManager, null)
    }

    // ------ //

    /** ボトムバーの項目をセットするダイアログを表示する */
    fun openBottomBarItemSetterDialog(
        args: UserBottomItemsSetter.OnMenuItemClickArguments,
        fragmentManager: FragmentManager,
        tag: String? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        BottomBarItemSelectionDialog.createInstance(args.items, args.target).run {
            showAllowingStateLoss(fragmentManager, tag)

            setOnSelectItemListener { (position, old, new) ->
                onSelectItem(position, old, new)
            }

            setOnReorderItemListener { (posA, posB, itemA, itemB) ->
                onReorderItem(posA, posB, itemA, itemB!!)
            }
        }
    }

    /** ボトムバーの項目を追加・編集 */
    private fun onSelectItem(position: Int, old: UserBottomItem?, new: UserBottomItem?) {
        if (old == null && new == null) return
        else if (new == null) {
            bottomBarButtons.value = bottomBarButtons.value?.minus(old!!)
        }
        else if (old == null) {
            bottomBarButtons.value = (bottomBarButtons.value ?: emptyList()).plus(new)
        }
        else {
            bottomBarButtons.value = bottomBarButtons.value?.mapIndexed { idx, item ->
                if (idx == position) new
                else item
            }
        }
    }

    /** ボトムバーの項目を入れ替え */
    private fun onReorderItem(
        positionA: Int,
        positionB: Int,
        itemA: UserBottomItem?,
        itemB: UserBottomItem
    ) {
        val items = bottomBarButtons.value ?: emptyList()
        if (itemA == null) {
            // 末尾に移動
            bottomBarButtons.value = items.minus(itemB).plus(itemB)
        }
        else {
            // 項目同士の入れ替え
            bottomBarButtons.value = items.mapIndexed { idx, item ->
                when (idx) {
                    positionA -> itemB
                    positionB -> itemA
                    else -> item
                }
            }
        }
    }

    // ------ //

    /**
     * 既読マークをつけるタイミングを選択する
     */
    private fun openReadEntryConditionSelectionDialog(fragmentManager: FragmentManager) {
        val values = ReadEntryCondition.visibleValues()
        val labelIds = values.map { it.textId }
        val value = readMarkCondition.value.int
        val states = values.map { it.int and value > 0 }.toBooleanArray()

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_entries_read_mark_condition_desc)
            .setMultipleChoiceItems(labelIds, states) { _, which, state ->
                states[which] = state
            }
            .setPositiveButton(R.string.dialog_ok) {
                var code = 0
                for (i in values.indices) {
                    code = code or (values[i].int and states[i])
                }
                readMarkCondition.value = ReadEntryCondition.fromInt(code)
            }
            .show(fragmentManager, null)
    }

    /**
     * 既読エントリを非表示にするカテゴリ選択ダイアログ
     */
    private fun openCategoriesHidingReadEntriesSelectionDialog(fragmentManager: FragmentManager) {
        val prevCategories = categoriesHidingReadEntries.value
        val values = Category.values().filter { it.canHideReadEntries }
        val labelIds = values.map { it.textId }
        val states = values.map { prevCategories.contains(it) }.toBooleanArray()

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_entries_categories_hiding_read_entries_desc)
            .setMultipleChoiceItems(labelIds, states) { _, which, state ->
                states[which] = state
            }
            .setPositiveButton(R.string.dialog_ok) {
                categoriesHidingReadEntries.value =
                    values.filterIndexed { i, _ -> states[i] }
            }
            .show(fragmentManager, null)
    }

    // ------ //

    /**
     * 「読んだ」時に定型文を投稿する場合の文章を設定するダイアログを開く
     */
    private fun openReadActionBoilerPlateEditingDialog(fragmentManager: FragmentManager) {
        val dialog = TextInputDialogFragment.createInstance(
            titleId = R.string.pref_entries_read_action_boilerplate_desc,
            hintId = R.string.pref_entries_read_action_boilerplate_hint,
            initialValue = entryReadActionBoilerPlate.value
        )

        dialog.setValidator {
            if (!BookmarkPostRepository.checkTagsCount(it)) {
                SatenaApplication.instance.showToast(
                    R.string.msg_post_too_many_tags,
                    BookmarkPostRepository.MAX_TAGS_COUNT
                )
                return@setValidator false
            }

            if (!BookmarkPostRepository.checkCommentLength(it)) {
                SatenaApplication.instance.showToast(
                    R.string.msg_comment_too_long,
                    BookmarkPostRepository.MAX_COMMENT_LENGTH
                )
                return@setValidator false
            }

            return@setValidator true
        }

        dialog.setOnCompleteListener {
            entryReadActionBoilerPlate.value = it
        }

        dialog.show(fragmentManager, null)
    }

    // ------ //

    /**
     * ボトムメニューの項目編集用ビュー
     */
    class PrefItemBottomMenuSetter(
        @StringRes private val titleId : Int,
        private val viewModel : EntryViewModel,
        private val fragmentManager: FragmentManager
    ) : PreferencesAdapter.Item {
        override val layoutId: Int
            get() = R.layout.listview_item_prefs_bottom_items

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsBottomItemsBinding> {
                it.vm = viewModel
                it.titleId = titleId

                it.itemSetter.setOnMenuItemClickListener { args ->
                    viewModel.openBottomBarItemSetterDialog(args, fragmentManager)
                }
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemBottomMenuSetter && new is PrefItemBottomMenuSetter &&
                    old.titleId == new.titleId

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemBottomMenuSetter && new is PrefItemBottomMenuSetter &&
                    old.fragmentManager == new.fragmentManager &&
                    old.titleId == new.titleId &&
                    old.viewModel == new.viewModel
    }
}
