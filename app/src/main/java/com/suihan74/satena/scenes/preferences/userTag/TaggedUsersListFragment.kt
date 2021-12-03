package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentTaggedUsersListBinding
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesTab
import com.suihan74.utilities.bindings.setDivider

class TaggedUsersListFragment : Fragment() {
    companion object {
        fun createInstance() = TaggedUsersListFragment()
    }

    // ------ //

    private val viewModel by viewModels<UserTagViewModel>({ requireParentFragment() })
    private val activityViewModel by activityViewModels<PreferencesActivity.ActivityViewModel>()

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))

        exitTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTaggedUsersListBinding.inflate(
            inflater,
            container,
            false
        )

        val taggedUsersAdapter = object : TaggedUsersAdapter() {
            override fun onItemClicked(user: User) {
                viewModel.openUserMenuDialog(requireActivity(), user, parentFragmentManager)
            }

            override fun onItemLongClicked(user: User): Boolean {
                onItemClicked(user)
                return true
            }
        }

        binding.usersList.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = taggedUsersAdapter
        }

        viewModel.currentTag.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer
            binding.tagName.text = it.userTag.name
            binding.usersCount.text = String.format("%d users", it.users.size)
            taggedUsersAdapter.setItems(it.users)
        })

        // 戻るボタンで閉じる
        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.currentTag.value = null
            parentFragmentManager.popBackStack()
            remove()
        }
        activityViewModel.currentTab.observe(viewLifecycleOwner, {
            callback.isEnabled = it == PreferencesTab.USER_TAGS
        })

        return binding.root
    }
}
