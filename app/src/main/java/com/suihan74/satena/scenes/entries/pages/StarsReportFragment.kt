package com.suihan74.satena.scenes.entries.pages

import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.StarsFragmentBase

class StarsReportFragment : StarsFragmentBase() {
    companion object {
        fun createInstance() = StarsReportFragment()
    }

    override val title = SatenaApplication.instance.getString(R.string.category_stars_report) ?: ""

    override val currentCategory = Category.StarsReport

    override fun refreshEntries() =
        refreshEntries(HatenaClient.getRecentStarsReportAsync())
}
