package com.suihan74.satena.scenes.entries2.pages

import android.view.Menu
import android.view.MenuInflater
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class UserEntriesFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String) = UserEntriesFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, Category.User)
        }
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
    }


    override fun initializeRecyclerView(
        entriesList: RecyclerView,
        swipeLayout: SwipeRefreshLayout
    ) {
        // TODO: "Not yet implemented"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        val activity = requireActivity()
        inflater.inflate(R.menu.spinner_issues, menu)
        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, emptyList(), R.drawable.spinner_allow_tags, getString(R.string.desc_issues_spinner)) { position ->
                // TODO: Tagスピナーの挙動
            }
        }
    }
}
