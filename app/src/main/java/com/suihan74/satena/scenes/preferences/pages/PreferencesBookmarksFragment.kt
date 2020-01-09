package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesBookmarksBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_bookmarks.view.*

/** ブクマ設定画面用のBindingAdapter */
object PreferencesBookmarksAdapter {
    /** 「最初に表示するタブ」のボタンテキスト */
    @BindingAdapter("app:bookmarksTabType")
    @JvmStatic
    fun setBookmarksTabTypeText(button: Button, ordinal: Int) {
        val tab = BookmarksTabType.fromInt(ordinal)
        button.setText(tab.textId)
    }

    /** 「リンク文字列をタップしたときの動作」のボタンテキスト */
    @BindingAdapter("app:linkTapAction")
    @JvmStatic
    fun setLinkTapActionText(button: Button, ordinal: Int) {
        val act = TapEntryAction.fromInt(ordinal)
        button.setText(act.titleId)
    }
}

class PreferencesBookmarksFragment :
    PreferencesFragmentBase(),
    AlertDialogFragment.Listener
{
    companion object {
        fun createInstance() = PreferencesBookmarksFragment()

        // dialog tags
        private const val DIALOG_INITIAL_TAB = "DIALOG_INITIAL_TAB"
        private const val DIALOG_LINK_SINGLE_TAP_ACTION = "DIALOG_LINK_SINGLE_TAP_ACTION"
        private const val DIALOG_LINK_LONG_TAP_ACTION = "DIALOG_LINK_LONG_TAP_ACTION"
    }

    private lateinit var viewModel: PreferencesBookmarksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val factory = PreferencesBookmarksViewModel.Factory(prefs)
        viewModel = ViewModelProviders.of(this, factory)[PreferencesBookmarksViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesBookmarksBinding>(
            inflater,
            R.layout.fragment_preferences_bookmarks,
            container,
            false
        ).apply {
            vm = viewModel
        }
        val view = binding.root

        // 最初に表示するタブ
        view.button_initial_tab.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_bookmarks_initial_tab_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    BookmarksTabType.values().map {
                        requireContext().getString(it.textId)
                    },
                    viewModel.initialTabPosition.value!!
                )
                .show(childFragmentManager, DIALOG_INITIAL_TAB)
        }

        // タップアクションの設定項目を初期化する処理
        val initializeTapActionSelector = { viewId: Int, descId: Int, tag: String ->
            view.findViewById<Button>(viewId).apply {
                setOnClickListener {
                    val selectedAction =
                        when (tag) {
                            DIALOG_LINK_SINGLE_TAP_ACTION -> viewModel.linkSingleTapAction.value!!
                            DIALOG_LINK_LONG_TAP_ACTION -> viewModel.linkLongTapAction.value!!
                            else -> throw RuntimeException("invalid dialog tag")
                        }

                    AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                        .setTitle(descId)
                        .setNegativeButton(R.string.dialog_cancel)
                        .setSingleChoiceItems(
                            TapEntryAction.values().map { getString(it.titleId) },
                            selectedAction
                        )
                        .show(childFragmentManager, tag)
                }
            }
        }

        initializeTapActionSelector(
            R.id.button_link_single_tap_action,
            R.string.pref_bookmark_link_single_tap_action_desc,
            DIALOG_LINK_SINGLE_TAP_ACTION
        )

        initializeTapActionSelector(
            R.id.button_link_long_tap_action,
            R.string.pref_bookmark_link_long_tap_action_desc,
            DIALOG_LINK_LONG_TAP_ACTION
        )

        // --- observers --- //

        viewModel.initialTabPosition.observe(this, Observer {
            PreferencesBookmarksAdapter.setBookmarksTabTypeText(view.button_initial_tab, it)
        })

        viewModel.linkSingleTapAction.observe(this, Observer {
            PreferencesBookmarksAdapter.setLinkTapActionText(view.button_link_single_tap_action, it)
        })

        viewModel.linkLongTapAction.observe(this, Observer {
            PreferencesBookmarksAdapter.setLinkTapActionText(view.button_link_long_tap_action, it)
        })

        return view
    }

    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        when (dialog.tag) {
            DIALOG_INITIAL_TAB -> {
                viewModel.initialTabPosition.value = which
                dialog.dismiss()
            }

            DIALOG_LINK_SINGLE_TAP_ACTION -> {
                viewModel.linkSingleTapAction.value = which
                dialog.dismiss()
            }

            DIALOG_LINK_LONG_TAP_ACTION -> {
                viewModel.linkLongTapAction.value = which
                dialog.dismiss()
            }
        }
    }
}
