package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class SingleTabEntriesFragment : EntriesFragment() {
    companion object {
        fun createInstance(category: Category) = SingleTabEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, category)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_entries2_single, container, false)

        if (savedInstanceState == null) {
            val fragment =
                when (category) {
                    Category.Notices -> NoticesFragment.createInstance(viewModelKey)
                    else -> EntriesTabFragment.createInstance(viewModelKey, category)
                }

            childFragmentManager.beginTransaction()
                .replace(R.id.content_layout, fragment)
                .commit()
        }

        return root
    }
}
