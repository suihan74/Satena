package com.suihan74.satena.scenes.bookmarks.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.suihan74.satena.R

class DetailTabAdapter(
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    enum class TabType {
        STARS_TO_USER,
        STARS_FROM_USER,
        MENTION_TO_USER,
        MENTION_FROM_USER
        ;

        companion object {
            fun fromInt(i: Int) = values()[i]
        }
    }

    // TODO: ブクマ内容に合わせてタブを用意する

    override fun getCount() = 2

    override fun getItem(position: Int) : Fragment = Fragment()

    override fun getPageTitle(position: Int) = ""

    fun getPageTitleIcon(position: Int) = R.drawable.ic_star
}
