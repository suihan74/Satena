package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.bindings.setDivider
import kotlinx.coroutines.launch

class UserTagsListFragment : CoroutineScopeFragment() {
    private lateinit var model: UserTagViewModel
    private lateinit var mUserTagsAdapter : UserTagsAdapter

    var menuItems: Array<out Pair<String, (TagAndUsers)->Unit>>? = null
        private set

    companion object {
        fun createInstance() = UserTagsListFragment()

        const val DIALOG_TAG_MENU = "UserTagsListFragment.DIALOG_TAG_MENU"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(parentFragment!!)[UserTagViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_tags_list, container, false)

        val parentFragment = parentFragment as PreferencesUserTagsFragment

        menuItems = arrayOf(
            getString(R.string.pref_user_tags_tag_menu_edit) to { t -> this@UserTagsListFragment.modifyItem(t) },
            getString(R.string.pref_user_tags_tag_menu_remove) to { t -> this@UserTagsListFragment.removeItem(t) }
        )

        mUserTagsAdapter = object : UserTagsAdapter() {
            override fun onItemClicked(tag: TagAndUsers) {
                model.currentTag.postValue(tag)
                parentFragment.showTaggedUsersList()
            }

            override fun onItemLongClicked(tag: TagAndUsers): Boolean {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(tag.userTag.name)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(menuItems!!.map { it.first })
                    .setAdditionalData("tag", tag)
                    .show(parentFragment.childFragmentManager, DIALOG_TAG_MENU)

                return true
            }
        }

        root.findViewById<RecyclerView>(R.id.user_tags_list)?.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = mUserTagsAdapter
        }

        // リストの変更を監視
        model.tags.observe(this, Observer { tags ->
            mUserTagsAdapter.setItems(tags)
        })

        return root
    }

    private fun removeItem(tag: TagAndUsers) = launch {
        model.deleteTag(tag)
    }

    private fun modifyItem(tag: TagAndUsers) {
        UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
            .setUserTag(tag.userTag)
            .show(parentFragment!!.childFragmentManager, "modify_tag_dialog")
    }
}
