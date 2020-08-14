package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesBookmarksBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_bookmarks.view.*

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
        viewModel = ViewModelProvider(this, factory)[PreferencesBookmarksViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesBookmarksBinding>(
            inflater,
            R.layout.fragment_preferences_bookmarks,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
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
                .showAllowingStateLoss(childFragmentManager, DIALOG_INITIAL_TAB)
        }

        // タップアクションの設定項目を初期化する処理
        val initializeTapActionSelector = { viewId: Int, selectedActionLiveData: LiveData<Int>, descId: Int, tag: String ->
            view.findViewById<Button>(viewId).apply {
                setOnClickListener {
                    AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                        .setTitle(descId)
                        .setNegativeButton(R.string.dialog_cancel)
                        .setSingleChoiceItems(
                            TapEntryAction.values().map { getString(it.titleId) },
                            selectedActionLiveData.value!!
                        )
                        .showAllowingStateLoss(childFragmentManager, tag)
                }
            }
        }

        initializeTapActionSelector(
            R.id.button_link_single_tap_action,
            viewModel.linkSingleTapAction,
            R.string.pref_bookmark_link_single_tap_action_desc,
            DIALOG_LINK_SINGLE_TAP_ACTION
        )

        initializeTapActionSelector(
            R.id.button_link_long_tap_action,
            viewModel.linkLongTapAction,
            R.string.pref_bookmark_link_long_tap_action_desc,
            DIALOG_LINK_LONG_TAP_ACTION
        )

        return view
    }

    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        when (dialog.tag) {
            DIALOG_INITIAL_TAB -> {
                viewModel.initialTabPosition.value = which
            }

            DIALOG_LINK_SINGLE_TAP_ACTION -> {
                viewModel.linkSingleTapAction.value = which
            }

            DIALOG_LINK_LONG_TAP_ACTION -> {
                viewModel.linkLongTapAction.value = which
            }
        }
        dialog.dismiss()
    }
}

/** 「最初に表示するタブ」のボタンテキスト */
@BindingAdapter("bookmarksTabType")
fun Button.setBookmarksTabTypeText(ordinal: Int?) {
    if (ordinal == null) return
    val tab = BookmarksTabType.fromInt(ordinal)
    setText(tab.textId)
}

/** 「リンク文字列をタップしたときの動作」のボタンテキスト */
@BindingAdapter("linkTapAction")
fun Button.setLinkTapActionText(ordinal: Int?) {
    if (ordinal == null) return
    val act = TapEntryAction.fromInt(ordinal)
    setText(act.titleId)
}
