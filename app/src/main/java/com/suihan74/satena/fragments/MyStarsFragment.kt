package com.suihan74.satena.fragments

import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication

class MyStarsFragment : StarsFragmentBase() {
    companion object {
        fun createInstance() = MyStarsFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_mystars) ?: ""

    override fun refreshEntries() =
        refreshEntries(HatenaClient.getRecentStarsAsync())
}
