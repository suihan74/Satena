package com.suihan74.satena.scenes.preferences.userTag

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.*
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.models.TaggedUser
import com.suihan74.satena.models.UserTag
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.DividerItemDecorator

class TaggedUsersListFragment : Fragment() {
    private lateinit var mRoot: View
    private lateinit var mTaggedUsersAdapter : TaggedUsersAdapter
    private lateinit var mUserTag : UserTag

    companion object {
        fun createInstance(userTag: UserTag) = TaggedUsersListFragment().apply {
            mUserTag = userTag
        }

        private const val BUNDLE_USER_TAG_NAME = "mUserTag.name"
        const val DIALOG_TAG_MENU = "TaggedUsersListFragment.DIALOG_TAG_MENU"
    }

    var menuItems: Array<out Pair<String, (user: TaggedUser)->Unit>>? = null
        private set

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::mUserTag.isInitialized) {
            outState.putString(BUNDLE_USER_TAG_NAME, mUserTag.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(GravityCompat.END))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tagged_users_list, container, false)
        mRoot = root

        val parentFragment = parentFragment as PreferencesUserTagsFragment

        savedInstanceState?.run {
            val name = getString(BUNDLE_USER_TAG_NAME)!!
            mUserTag = parentFragment.userTagsContainer.getTag(name)!!
        }

        root.findViewById<TextView>(R.id.tag_name).text = mUserTag.name
        updateCounter()

        menuItems = arrayOf(
            getString(R.string.pref_user_tags_user_menu_show_entries) to { user -> showBookmarks(user) },
            getString(R.string.pref_user_tags_user_menu_remove) to { user -> removeItem(user) }
        )

        val userTagsContainer = parentFragment.userTagsContainer
        mTaggedUsersAdapter = object : TaggedUsersAdapter(userTagsContainer.getUsersOfTag(mUserTag)) {
            override fun onItemClicked(user: TaggedUser) {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(user.name)
                    .setItems(menuItems!!.map { it.first })
                    .setNegativeButton(R.string.dialog_cancel)
                    .setAdditionalData("user", user)
                    .show(parentFragment.childFragmentManager, DIALOG_TAG_MENU)
            }
        }

        root.findViewById<RecyclerView>(R.id.users_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(this@TaggedUsersListFragment.requireContext(),
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = mTaggedUsersAdapter
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.tagged_users_list, menu)

        menu.findItem(R.id.button).apply {
            val color = ActivityCompat.getColor(activity!!, R.color.colorPrimaryText)
            icon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

            setOnMenuItemClickListener {
                activity?.onBackPressed()
                true
            }
        }
    }

    private fun removeItem(user: TaggedUser) {
        val parentFragment = parentFragment as PreferencesUserTagsFragment
        mTaggedUsersAdapter.removeItem(user)
        parentFragment.removeUserFromTag(mUserTag, user)
        updateCounter()
    }

    fun addItem(user: TaggedUser) {
        mTaggedUsersAdapter.addItem(user)
        updateCounter()
    }

    fun updateCounter() {
        mRoot.findViewById<TextView>(R.id.users_count).text = String.format("%d users", mUserTag.count)
    }

    private fun showBookmarks(user: TaggedUser) {
        val intent = Intent(SatenaApplication.instance, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user.name)
        }
        startActivity(intent)
    }
}
