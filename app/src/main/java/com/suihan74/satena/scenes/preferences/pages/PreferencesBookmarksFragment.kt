package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesBookmarksBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.lazyProvideViewModel
import com.suihan74.utilities.showAllowingStateLoss

class PreferencesBookmarksFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesBookmarksFragment()

        // dialog tags
        private const val DIALOG_INITIAL_TAB = "DIALOG_INITIAL_TAB"
        private const val DIALOG_LINK_SINGLE_TAP_ACTION = "DIALOG_LINK_SINGLE_TAP_ACTION"
        private const val DIALOG_LINK_LONG_TAP_ACTION = "DIALOG_LINK_LONG_TAP_ACTION"
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        PreferencesBookmarksViewModel(
            SafeSharedPreferences.create<PreferenceKey>(context)
        )
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentPreferencesBookmarksBinding>(
            inflater,
            R.layout.fragment_preferences_bookmarks,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        // 最初に表示するタブ
        binding.buttonInitialTab.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_bookmarks_initial_tab_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    BookmarksTabType.values().map { it.textId },
                    viewModel.initialTabPosition.value!!
                ) { _, which ->
                    viewModel.initialTabPosition.value = which
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager, DIALOG_INITIAL_TAB)
        }

        initializeTapActionSelector(
            binding.buttonLinkSingleTapAction,
            viewModel.linkSingleTapAction,
            R.string.pref_bookmark_link_single_tap_action_desc,
            DIALOG_LINK_SINGLE_TAP_ACTION
        )

        initializeTapActionSelector(
            binding.buttonLinkLongTapAction,
            viewModel.linkLongTapAction,
            R.string.pref_bookmark_link_long_tap_action_desc,
            DIALOG_LINK_LONG_TAP_ACTION
        )

        return binding.root
    }

    /** タップアクションの設定項目を初期化する処理 */
    private fun initializeTapActionSelector(
        button: Button,
        selectedActionLiveData: LiveData<TapEntryAction>,
        descId: Int,
        tag: String
    ) {
        button.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(descId)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    TapEntryAction.values().map { it.titleId },
                    selectedActionLiveData.value!!.ordinal
                ) { _, which ->
                    when (tag) {
                        DIALOG_LINK_SINGLE_TAP_ACTION -> {
                            viewModel.linkSingleTapAction.value = TapEntryAction.fromOrdinal(which)
                        }

                        DIALOG_LINK_LONG_TAP_ACTION -> {
                            viewModel.linkLongTapAction.value = TapEntryAction.fromOrdinal(which)
                        }
                    }
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager, tag)
        }
    }
}

/** 「最初に表示するタブ」のボタンテキスト */
@BindingAdapter("bookmarksTabType")
fun Button.setBookmarksTabTypeText(ordinal: Int?) {
    if (ordinal == null) return
    val tab = BookmarksTabType.fromOrdinal(ordinal)
    setText(tab.textId)
}
