package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesBookmarksFragment : PreferencesFragmentBase(), AlertDialogFragment.Listener {
    companion object {
        fun createInstance() : PreferencesBookmarksFragment =
            PreferencesBookmarksFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_bookmarks, container, false)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        // 最初に表示するタブ
        view.findViewById<Button>(R.id.preferences_bookmarks_initial_tab).apply {
            val key = PreferenceKey.BOOKMARKS_INITIAL_TAB
            text = context!!.getText(BookmarksTabType.fromInt(prefs.getInt(key)).textId)
            setOnClickListener {
                val currentInitialTab = BookmarksTabType.fromInt(prefs.getInt(key))
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_bookmarks_initial_tab_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(
                        BookmarksTabType.values().map { context!!.getString(it.textId) },
                        currentInitialTab.ordinal
                    )
                    .show(childFragmentManager, "initial_tab_dialog")
            }
        }

        // ブックマークする前に確認する
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_using_post_dialog).apply {
            val key = PreferenceKey.USING_POST_BOOKMARK_DIALOG
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // スターをつける前に確認する
        view.findViewById<ToggleButton>(R.id.preferences_using_post_star_dialog).apply {
            val key = PreferenceKey.USING_POST_STAR_DIALOG
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // 画面スクロールで下部ボタン類を隠す
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_hiding_buttons_with_scrolling).apply {
            val key = PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // 「すべて」タブでは非表示ユーザーも表示する
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_showing_ignored_users_in_all_bookmarks).apply {
            val key = PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // idコールで言及されている非表示ユーザーを表示する
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_showing_ignored_users_with_calling).apply {
            val key = PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // ブコメ詳細画面で非表示ユーザーがつけたスターを表示する
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_showing_stars_of_ignored_users).apply {
            val key = PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // スクロールでツールバーを隠す
        view.findViewById<ToggleButton>(R.id.preferences_bookmarks_hiding_toolbar_by_scrolling).apply {
            val key = PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // タップアクションの設定項目を初期化する処理
        val initializeTapActionSelector = { viewId: Int, key: PreferenceKey, descId: Int ->
            view.findViewById<Button>(viewId).apply {
                text = getText(TapEntryAction.fromInt(prefs.getInt(key)).titleId)
                setOnClickListener {
                    val selectedAction = TapEntryAction.fromInt(prefs.getInt(key))
                    AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                        .setTitle(descId)
                        .setNegativeButton(R.string.dialog_cancel)
                        .setSingleChoiceItems(
                            TapEntryAction.values().map { getString(it.titleId) },
                            selectedAction.ordinal)
                        .setAdditionalData("key", key)
                        .setAdditionalData("view_id", viewId)
                        .show(childFragmentManager, "tap_action_dialog")
                }
            }
        }

        // ブコメ内のリンクをタップしたときの挙動
        initializeTapActionSelector(
            R.id.preferences_bookmark_link_single_tap_action,
            PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION,
            R.string.pref_bookmark_link_single_tap_action_desc)

        // ブコメ内のリンクを長押ししたときの挙動
        initializeTapActionSelector(
            R.id.preferences_bookmark_link_long_tap_action,
            PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION,
            R.string.pref_bookmark_link_long_tap_action_desc)

        return view
    }

    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        when (dialog.tag) {
            "initial_tab_dialog" -> {
                val tab = BookmarksTabType.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.BOOKMARKS_INITIAL_TAB, tab.ordinal)
                }
                view?.findViewById<Button>(R.id.preferences_bookmarks_initial_tab)?.run {
                    text = context!!.getText(tab.textId)
                }
                dialog.dismiss()
            }

            "tap_action_dialog" -> {
                val key = dialog.getAdditionalData<PreferenceKey>("key") ?: return
                val viewId = dialog.getAdditionalData<Int>("view_id") ?: return
                val selectedAction = TapEntryAction.fromInt(which)
                prefs.edit {
                    putInt(key, selectedAction.ordinal)
                }
                view?.findViewById<Button>(viewId)?.run {
                    text = dialog.items!![which]
                }
                dialog.dismiss()
            }
        }
    }
}
