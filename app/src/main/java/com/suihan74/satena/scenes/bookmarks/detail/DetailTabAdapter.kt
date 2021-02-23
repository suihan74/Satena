package com.suihan74.satena.scenes.bookmarks.detail

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.detail.tabs.StarRelationsTabFragment
import com.suihan74.utilities.IconFragmentStateAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailTabAdapter(
    fragment : Fragment,
    viewModel : BookmarkDetailViewModel,
    val bookmark : Bookmark
) : IconFragmentStateAdapter(fragment) {

    private var tabs = listOf(
        TabType.STARS_TO_USER,
        TabType.STARS_FROM_USER
    )

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int) = tabs[position].createFragment()

    override fun getIconId(position: Int) = tabs[position].iconId

    override fun getTitleId(position: Int) = tabs[position].titleId

    override fun getTooltipTextId(position: Int): Int = tabs[position].tooltipTextId

    override fun getTitle(context: Context, position: Int, textId: Int): CharSequence {
        val count = when (tabs[position]) {
            TabType.STARS_TO_USER -> starsToUserCount
            TabType.STARS_FROM_USER -> starsFromUserCount
            TabType.MENTION_TO_USER -> mentionsToUserCount
            TabType.MENTION_FROM_USER -> mentionsFromUserCount
        }
        return context.getString(textId, count, bookmark.user)
    }

    override fun getTooltipText(context: Context, position: Int, textId: Int): CharSequence {
        return context.getString(textId, bookmark.user)
    }

    // ------ //

    private var starsToUserCount = 0

    private var starsFromUserCount = 0

    private var mentionsToUserCount = 0

    private var mentionsFromUserCount = 0

    // ------ //

    init {
        val lifecycleOwner = fragment.viewLifecycleOwner
        val lifecycleScope = fragment.lifecycleScope
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            viewModel.starsToUser.observe(lifecycleOwner, { list ->
                lifecycleScope.launch(Dispatchers.Default) {
                    starsToUserCount = list.sumBy { it.star?.count ?: 0 }
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                    }
                }
            })

            viewModel.starsFromUser.observe(lifecycleOwner, { list ->
                lifecycleScope.launch(Dispatchers.Default) {
                    starsFromUserCount = list.sumBy { it.star?.count ?: 0 }
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                    }
                }
            })

            viewModel.mentionsToUser.observe(lifecycleOwner, { list ->
                if (!list.isNullOrEmpty() && !tabs.contains(TabType.MENTION_TO_USER)) {
                    tabs = tabs.plus(TabType.MENTION_TO_USER)
                }
                mentionsToUserCount = list.size
                notifyDataSetChanged()
            })

            viewModel.mentionsFromUser.observe(lifecycleOwner, { list ->
                if (!list.isNullOrEmpty() && !tabs.contains(TabType.MENTION_FROM_USER)) {
                    tabs = tabs.plus(TabType.MENTION_FROM_USER)
                }
                mentionsFromUserCount = list.size
                notifyDataSetChanged()
            })
        }
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
            { StarRelationsTabFragment.createInstance(MENTION_TO_USER) }
        ),

        MENTION_FROM_USER(
            R.drawable.ic_baseline_chat_bubble,
            R.string.bookmark_detail_tab_mentions_from,
            R.string.bookmark_detail_tab_tooltip_mentions_from,
            { StarRelationsTabFragment.createInstance(MENTION_FROM_USER) }
        )
        ;

        companion object {
            fun fromOrdinal(i: Int) = values().getOrElse(i) { STARS_TO_USER }
        }
    }
}
