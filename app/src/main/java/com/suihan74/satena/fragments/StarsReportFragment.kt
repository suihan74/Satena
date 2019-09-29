package com.suihan74.satena.fragments

import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication

class StarsReportFragment : StarsFragmentBase() {
    companion object {
        fun createInstance() = StarsReportFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_stars_report) ?: ""

    override fun refreshEntries() =
        refreshEntries(HatenaClient.getRecentStarsReportAsync())
}
