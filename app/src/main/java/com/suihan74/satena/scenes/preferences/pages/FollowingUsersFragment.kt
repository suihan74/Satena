package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesFollowingUsersBinding
import com.suihan74.satena.scenes.preferences.ignored.FollowingUsersViewModel
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter
import com.suihan74.satena.scenes.preferences.ignored.UserRelationRepository
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.lazyProvideViewModel
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

    private val viewModel by lazyProvideViewModel {
        val repository = UserRelationRepository(
            SatenaApplication.instance.accountLoader
        )
        FollowingUsersViewModel(repository)
    }

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

        // ID検索ボタン
        binding.searchButton.setOnClickListener {
            viewModel.isFilterTextVisible.value =
                viewModel.isFilterTextVisible.value != true
        }

        viewModel.isFilterTextVisible.observe(viewLifecycleOwner) {
            if (it != true) {
                binding.searchText.text?.clear()
            }
        }

        // ユーザーリスト
        val ignoredUsersAdapter = IgnoredUsersAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnClickItemListener { binding ->
                binding.user?.let { user ->
                    viewModel.openMenuDialog(activity, user, childFragmentManager)
                }
            }
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
}
