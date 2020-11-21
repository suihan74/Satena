package com.suihan74.satena.scenes.bookmarks.detail

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.detail.tabs.StarRelationsTabFragment
import com.suihan74.utilities.IconFragmentPagerAdapter

class DetailTabAdapter(
    val bookmark : Bookmark,
    fragmentManager : FragmentManager
) : IconFragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var tabs = listOf(
        TabType.STARS_TO_USER,
        TabType.STARS_FROM_USER
    )

    override fun getCount() = tabs.size

    override fun getItem(position: Int) = tabs[position].createFragment()

    override fun getIconId(position: Int) = tabs[position].iconId

    override fun getTitleId(position: Int) = tabs[position].titleId

    override fun getTooltipTextId(position: Int): Int = tabs[position].tooltipTextId

    override fun getTitle(context: Context, position: Int, textId: Int): CharSequence {
        return context.getString(textId, bookmark.user)
    }

    override fun getTooltipText(context: Context, position: Int, textId: Int): CharSequence {
        return context.getString(textId, bookmark.user)
    }

    // ------ //

    enum class TabType(
        @DrawableRes val iconId: Int,
        @StringRes val titleId: Int,
        @StringRes val tooltipTextId: Int,
        val createFragment : ()->Fragment
    ) {
        STARS_TO_USER(
            R.drawable.ic_star,
            R.string.bookmark_detail_tab_stars_to,
            R.string.bookmark_detail_tab_tooltip_stars_to,
            { StarRelationsTabFragment.createInstance(STARS_TO_USER) }
        ),

        STARS_FROM_USER(
            R.drawable.ic_star,
            R.string.bookmark_detail_tab_stars_from,
            R.string.bookmark_detail_tab_tooltip_stars_from,
            { StarRelationsTabFragment.createInstance(STARS_FROM_USER) }
        ),

        MENTION_TO_USER(
            R.drawable.ic_baseline_chat_bubble,
            R.string.bookmark_detail_tab_mentions_to,
            R.string.bookmark_detail_tab_tooltip_mentions_to,
            { Fragment() }
        ),

        MENTION_FROM_USER(
            R.drawable.ic_baseline_chat_bubble,
            R.string.bookmark_detail_tab_mentions_from,
            R.string.bookmark_detail_tab_tooltip_mentions_from,
            { Fragment() }
        )
        ;

        companion object {
            fun fromOrdinal(i: Int) = values().getOrElse(i) { STARS_TO_USER }
        }
    }
}
