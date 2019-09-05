package com.suihan74.satena.fragments

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R

class UserEntriesFragment : MultipurposeSingleTabEntriesFragment() {

    private lateinit var mUser : String

    companion object {
        fun createInstance(user: String) = UserEntriesFragment().apply {
            this.mUser = user
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("user", mUser)
    }

    override fun onRestoreSaveInstanceState(savedInstanceState: Bundle) {
        super.onViewStateRestored(savedInstanceState)
        mUser = savedInstanceState.getString("user")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!

        root.findViewById<Toolbar>(R.id.toolbar).apply {
            title = "${mUser}のブックマーク"
        }

        refreshEntries()
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries("${mUser}のブックマークエントリの取得失敗") { offset -> HatenaClient.getUserEntriesAsync(mUser, of = offset) }
}
