package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredUsersBinding
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersRepository
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesIgnoredUsersFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesIgnoredUsersFragment()
    }

    private val viewModel by lazyProvideViewModel {
        val repository = IgnoredUsersRepository(
            SatenaApplication.instance.accountLoader
        )
        PreferencesIgnoredUsersViewModel(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentPreferencesIgnoredUsersBinding>(
            inflater,
            R.layout.fragment_preferences_ignored_users,
            container,
            false
        ).also {
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

        binding.ignoredUsersList.also { list ->
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
