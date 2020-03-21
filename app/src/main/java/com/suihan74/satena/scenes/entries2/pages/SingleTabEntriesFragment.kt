package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase

abstract class SingleTabEntriesFragment : EntriesFragment() {
    /** コンテンツ部分に表示するフラグメントを生成する */
    abstract fun generateContentFragment() : EntriesTabFragmentBase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_entries2_single, container, false)

        if (savedInstanceState == null) {
            val fragment = generateContentFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.content_layout, fragment)
                .commit()
        }

        return root
    }
}
