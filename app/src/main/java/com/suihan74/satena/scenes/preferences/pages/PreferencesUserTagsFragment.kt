package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesUserTagsBinding
import com.suihan74.satena.scenes.preferences.userTag.TaggedUsersListFragment
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagViewModel
import com.suihan74.satena.scenes.preferences.userTag.UserTagsListFragment
import com.suihan74.utilities.lazyProvideViewModel

class PreferencesUserTagsFragment : Fragment() {
    companion object {
        fun createInstance() =
            PreferencesUserTagsFragment()
    }

    // ------ //

    val viewModel by lazyProvideViewModel {
        UserTagViewModel(
            UserTagRepository(SatenaApplication.instance.userTagDao)
        )
    }

    // ------ //

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentPreferencesUserTagsBinding.inflate(
            inflater,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        viewModel.init(
            onFinally = { initializeViews(binding) }
        )

        return binding.root
    }

    // ------ //

    /** ビューの初期化 */
    private fun initializeViews(binding: FragmentPreferencesUserTagsBinding) {
        // タグ一覧を表示
        showUserTagsList()
        if (viewModel.currentTag.value != null) {
            showTaggedUsersList()
        }

        binding.addButton.setOnClickListener {
            val userTagsList = getCurrentFragment<UserTagsListFragment>()
            if (userTagsList != null) {
                viewModel.openUserTagDialog(null, childFragmentManager)
            }
            else {
                val taggedUsersList = getCurrentFragment<TaggedUsersListFragment>()
                if (taggedUsersList != null) {
                    viewModel.openTagUserDialog(childFragmentManager)
                }
            }
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ------ //

    fun showTaggedUsersList() {
        val fragment = TaggedUsersListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun showUserTagsList() {
        val fragment = UserTagsListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .commitAllowingStateLoss()
    }

    // ------ //

    private inline fun <reified T> getCurrentFragment() : T? {
        val fragment = childFragmentManager.findFragmentById(R.id.content_layout)
        @Suppress("UNCHECKED_CAST")
        return fragment as? T
    }
}
