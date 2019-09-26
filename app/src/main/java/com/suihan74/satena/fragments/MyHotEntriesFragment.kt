package com.suihan74.satena.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.HatenaClient

class MyHotEntriesFragment : MultipurposeSingleTabEntriesFragment() {
    companion object {
        fun createInstance() = MyHotEntriesFragment()
    }

    override val title = "マイホットエントリ"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        refreshEntries()
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries("マイホットエントリの取得失敗") { HatenaClient.getMyHotEntriesAsync() }
}
