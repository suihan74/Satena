package com.suihan74.satena.scenes.entries.pages

import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.entries.StarsFragmentBase

class MyStarsFragment : StarsFragmentBase() {
    companion object {
        fun createInstance() = MyStarsFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_mystars) ?: ""

    override fun refreshEntries() =
        refreshEntries(HatenaClient.getRecentStarsAsync())
}
