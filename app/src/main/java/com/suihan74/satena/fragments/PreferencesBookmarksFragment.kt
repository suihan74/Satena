package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.R

class PreferencesBookmarksFragment : Fragment() {
    companion object {
        fun createInstance() : PreferencesBookmarksFragment =
            PreferencesBookmarksFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_bookmarks, container, false)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)

        // 最初に表示するタブ
        view.findViewById<Button>(R.id.preferences_bookmarks_initial_tab).apply {
            val key = PreferenceKey.BOOKMARKS_INITIAL_TAB
            var currentInitialTab = BookmarksTabType.fromInt(prefs.getInt(key))
            text = currentInitialTab.toString(context!!)
            setOnClickListener {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getString(R.string.pref_bookmarks_initial_tab_desc))
                    .setSingleChoiceItems(BookmarksTabType.values().map { it.toString(context!!) }.toTypedArray(), currentInitialTab.int) { dialog, which ->
                        val tab = BookmarksTabType.fromInt(which)
                        prefs.edit {
                            putInt(key, tab.int)
                        }
                        currentInitialTab = tab
                        this.text = tab.toString(context!!)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
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

        return view
    }
}
