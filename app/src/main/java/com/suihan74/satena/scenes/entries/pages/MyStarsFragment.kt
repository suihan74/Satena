package com.suihan74.satena.scenes.entries.pages

import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.StarsFragmentBase

class MyStarsFragment : StarsFragmentBase() {
    companion object {
        fun createInstance() = MyStarsFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_mystars) ?: ""
    override val currentCategory = Category.Stars

    override fun refreshEntries() =
        refreshEntries(HatenaClient.getRecentStarsAsync())
}
