package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesFollowingUsersBinding
import com.suihan74.satena.databinding.ListviewItemIgnoredUsersBinding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.ignored.FollowingUsersViewModel
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter
import com.suihan74.utilities.extensions.getThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * フォロー中ユーザーリスト
 */
class FollowingUsersFragment : Fragment() {
    companion object {
        fun createInstance() = FollowingUsersFragment()
    }

    // ------ //

    private val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    private val viewModel
        get() = preferencesActivity.followingsViewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesFollowingUsersBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        val activity = requireActivity()

        // ユーザーリスト
        val ignoredUsersAdapter = IgnoredUsersAdapter(viewLifecycleOwner).also { adapter ->
            val listener = { binding : ListviewItemIgnoredUsersBinding ->
                binding.user?.let { user ->
                    viewModel.openMenuDialog(user, childFragmentManager)
                }
                Unit
            }
            adapter.setOnClickItemListener(listener)
            adapter.setOnLongLickItemListener(listener)
        }

        binding.usersList.also { list ->
            list.adapter = ignoredUsersAdapter
            list.setHasFixedSize(true)
        }

        // スワイプ更新機能の設定
        binding.swipeLayout.also { swipeLayout ->
            swipeLayout.setProgressBackgroundColorSchemeColor(
                activity.getThemeColor(R.attr.swipeRefreshBackground)
            )
            swipeLayout.setColorSchemeColors(
                activity.getThemeColor(R.attr.colorPrimary)
            )
            swipeLayout.setOnRefreshListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    viewModel.loadList()
                    swipeLayout.isRefreshing = false
                }
            }
        }

        binding.modeToggleButton.apply {
            lifecycleScope.launchWhenResumed {
                check(modeToCheckedId(viewModel.mode))
            }
            isSingleSelection = true
            isSelectionRequired = true
            addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                viewModel.setMode(checkedIdToMode(checkedId), viewLifecycleOwner)
            }
        }

        // 戻るボタンで検索部分を閉じる
        val onBackCallback = activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            viewModel.isFilterTextVisible.value == true
        ) {
            viewModel.isFilterTextVisible.value = false
        }

        viewModel.isFilterTextVisible.observe(viewLifecycleOwner) {
            onBackCallback.isEnabled = it
        }

        return binding.root
    }

    private fun checkedIdToMode(checkedId: Int) = when(checkedId) {
        R.id.followings_button -> FollowingUsersViewModel.Mode.FOLLOWINGS
        R.id.followers_button -> FollowingUsersViewModel.Mode.FOLLOWERS
        else -> throw IllegalStateException()
    }

    private fun modeToCheckedId(mode: FollowingUsersViewModel.Mode) = when(mode) {
        FollowingUsersViewModel.Mode.FOLLOWINGS -> R.id.followings_button
        FollowingUsersViewModel.Mode.FOLLOWERS -> R.id.followers_button
        else -> throw IllegalStateException()
    }
}
