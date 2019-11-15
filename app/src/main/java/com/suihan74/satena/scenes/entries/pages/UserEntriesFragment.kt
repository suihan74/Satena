package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.scenes.entries.SingleTabEntriesFragmentBase

class UserEntriesFragment : SingleTabEntriesFragmentBase() {

    private lateinit var mUser : String
    override val title: String
        get() = "${mUser}のブックマーク"

    companion object {
        fun createInstance(user: String) = UserEntriesFragment().apply {
            this.mUser = user
        }

        private const val BUNDLE_USER = "mUser"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_USER, mUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            mUser = it.getString(BUNDLE_USER)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        refreshEntries()
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries("${mUser}のブックマークエントリの取得失敗") { offset -> HatenaClient.getUserEntriesAsync(mUser, of = offset) }
}
