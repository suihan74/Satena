package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredUsersBinding
import com.suihan74.satena.databinding.ListviewItemIgnoredUsersBinding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter

class IgnoredUsersFragment : Fragment() {
    companion object {
        fun createInstance() = IgnoredUsersFragment()
    }

    // ------ //

    private val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    private val viewModel
        get() = preferencesActivity.ignoredUsersViewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesIgnoredUsersBinding.inflate(inflater, container, false).also {
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
            val listener = { binding: ListviewItemIgnoredUsersBinding ->
                binding.user?.let { user ->
                    viewModel.openMenuDialog(activity, user, childFragmentManager)
                }
                Unit
            }
            adapter.setOnClickItemListener(listener)
            adapter.setOnLongLickItemListener(listener)
        }

        binding.ignoredUsersList.also { list ->
            list.adapter = ignoredUsersAdapter
            list.setHasFixedSize(true)
        }

        // スワイプ更新機能の設定
        binding.swipeLayout.setOnRefreshListener {
            lifecycleScope.launchWhenResumed {
                viewModel.loadList()
                binding.swipeLayout.isRefreshing = false
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
