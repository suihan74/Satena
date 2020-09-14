package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.DrawableCompat
import com.suihan74.utilities.bindings.setDivider
import kotlinx.android.synthetic.main.fragment_tagged_users_list.view.*

class TaggedUsersListFragment : Fragment() {
    private val userTagsFragment : PreferencesUserTagsFragment
        get() = requireParentFragment() as PreferencesUserTagsFragment

    private val viewModel : UserTagViewModel
        get() = userTagsFragment.viewModel

    companion object {
        fun createInstance() = TaggedUsersListFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tagged_users_list, container, false)

        val taggedUsersAdapter = object : TaggedUsersAdapter() {
            override fun onItemClicked(user: User) {
                viewModel.openUserMenuDialog(requireActivity(), user, userTagsFragment.childFragmentManager)
            }

            override fun onItemLongClicked(user: User): Boolean {
                onItemClicked(user)
                return true
            }
        }

        root.users_list?.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = taggedUsersAdapter
        }

        viewModel.currentTag.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            root.tag_name.text = it.userTag.name
            root.users_count.text = String.format("%d users", it.users.size)
            taggedUsersAdapter.setItems(it.users)
        }

        return root
    }


    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.tagged_users_list, menu)

        menu.findItem(R.id.button).apply {
            val color = ActivityCompat.getColor(requireActivity(), R.color.colorPrimaryText)
            DrawableCompat.setColorFilter(icon.mutate(), color)

            setOnMenuItemClickListener {
                activity?.onBackPressed()
                true
            }
        }
    }
}
