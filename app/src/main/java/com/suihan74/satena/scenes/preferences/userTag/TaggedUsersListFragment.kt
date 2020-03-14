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
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.bindings.setDivider
import kotlinx.coroutines.launch

class TaggedUsersListFragment : CoroutineScopeFragment() {
    private lateinit var mTaggedUsersAdapter : TaggedUsersAdapter

    private lateinit var model: UserTagViewModel

    companion object {
        fun createInstance() = TaggedUsersListFragment()

        const val DIALOG_TAG_MENU = "TaggedUsersListFragment.DIALOG_TAG_MENU"
    }

    var menuItems: Array<out Pair<String, (user: User)->Unit>>? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(GravityCompat.END))

        val parentFragment = requireParentFragment() as PreferencesUserTagsFragment
        model = ViewModelProvider(parentFragment)[UserTagViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tagged_users_list, container, false)

        val parentFragment = parentFragment as PreferencesUserTagsFragment

        menuItems = arrayOf(
            getString(R.string.pref_user_tags_user_menu_show_entries) to { user -> showBookmarks(user) },
            getString(R.string.pref_user_tags_user_menu_remove) to { user -> removeItem(user) }
        )

        mTaggedUsersAdapter = object : TaggedUsersAdapter() {
            override fun onItemClicked(user: User) {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(user.name)
                    .setItems(menuItems!!.map { it.first })
                    .setNegativeButton(R.string.dialog_cancel)
                    .setAdditionalData("user", user)
                    .show(parentFragment.childFragmentManager, DIALOG_TAG_MENU)
            }

            override fun onItemLongClicked(user: User): Boolean {
                onItemClicked(user)
                return true
            }
        }

        root.findViewById<RecyclerView>(R.id.users_list)?.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = mTaggedUsersAdapter
        }

        model.currentTag.observe(this, Observer {
            if (it == null) return@Observer
            root.findViewById<TextView>(R.id.tag_name).text = it.userTag.name
            root.findViewById<TextView>(R.id.users_count).text = String.format("%d users", it.users.size)
            mTaggedUsersAdapter.setItems(it.users)
        })

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
            val color = ActivityCompat.getColor(requireActivity(), R.color.colorPrimaryText)
            icon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

            setOnMenuItemClickListener {
                activity?.onBackPressed()
                true
            }
        }
    }

    private fun removeItem(user: User) = launch {
        val tag = model.currentTag.value?.userTag ?: return@launch
        model.deleteRelation(tag, user)
    }

    private fun showBookmarks(user: User) {
        val intent = Intent(SatenaApplication.instance, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user.name)
        }
        startActivity(intent)
    }
}
