package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.SingleTabEntriesFragmentBase

class MyHotEntriesFragment : SingleTabEntriesFragmentBase() {
    companion object {
        fun createInstance() = MyHotEntriesFragment()
    }

    override val title get() = SatenaApplication.instance.getString(R.string.category_myhotentries)

    override val currentCategory = Category.MyHotEntries

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        refreshEntries()
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries("マイホットエントリの取得失敗") { HatenaClient.getMyHotEntriesAsync() }
}
