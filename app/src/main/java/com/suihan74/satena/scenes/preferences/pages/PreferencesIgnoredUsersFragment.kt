package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredUsersBinding
import com.suihan74.satena.scenes.browser.bookmarks.IgnoredUsersRepository
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.BackPressable
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.provideViewModel
import kotlinx.android.synthetic.main.fragment_preferences_ignored_users.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesIgnoredUsersFragment : PreferencesFragmentBase(), BackPressable {
    companion object {
        fun createInstance() = PreferencesIgnoredUsersFragment()

        @JvmStatic
        @BindingAdapter("ignoredUsers")
        fun setIgnoredUsers(view: RecyclerView, items: List<String>?) {
            view.adapter.alsoAs<IgnoredUsersAdapter> { adapter ->
                if (!items.isNullOrEmpty()) {
                    adapter.setItems(items)
                }
            }
        }
    }

    private val viewModel by lazy {
        provideViewModel(this) {
            val repository = IgnoredUsersRepository(
                AccountLoader(
                    requireContext(),
                    HatenaClient,
                    MastodonClientHolder
                )
            )
            PreferencesIgnoredUsersViewModel(repository)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                binding.searchText.text.clear()
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

        return binding.root
    }

    override fun onBackPressed(): Boolean {
        val searchEditText = view?.search_text ?: return false
        return when(searchEditText.visibility) {
            View.VISIBLE -> {
                searchEditText.visibility = View.INVISIBLE
                searchEditText.text.clear()
                true
            }

            else -> false
        }
    }
}
