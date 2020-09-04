package com.suihan74.satena.scenes.bookmarks2.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.observe
import androidx.viewpager.widget.ViewPager
import com.suihan74.satena.R
import com.suihan74.utilities.Listener

class DetailTabAdapter(
    private val detailFragment: BookmarkDetailFragment
) : FragmentPagerAdapter(detailFragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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

    /** 実際に表示されているタブリスト */
    private val tabs : List<TabType>

    init {
        val tabsArrayList = arrayListOf(TabType.STARS_TO_USER, TabType.STARS_FROM_USER)
        tabs = tabsArrayList

        detailFragment.viewModel.mentionsToUser.observe(detailFragment.viewLifecycleOwner) {
            if (it.isNotEmpty() && !tabsArrayList.contains(TabType.MENTION_TO_USER)) {
                tabsArrayList.add(TabType.MENTION_TO_USER)
                notifyDataSetChanged()
            }
        }

        detailFragment.viewModel.mentionsFromUser.observe(detailFragment.viewLifecycleOwner) {
            if (it.isNotEmpty() && !tabsArrayList.contains(TabType.MENTION_FROM_USER)) {
                tabsArrayList.add(TabType.MENTION_FROM_USER)
                notifyDataSetChanged()
            }
        }
    }

    override fun getCount() = tabs.size

    override fun getItem(position: Int) : Fragment =
        when (tabs[position]) {
            TabType.STARS_TO_USER ->
                StarsToUserFragment.createInstance()

            TabType.STARS_FROM_USER ->
                StarsFromUserFragment.createInstance()

            TabType.MENTION_TO_USER ->
                MentionToUserFragment.createInstance()

            TabType.MENTION_FROM_USER ->
                MentionFromUserFragment.createInstance()
        }

    override fun getPageTitle(position: Int) =
        when (tabs[position]) {
            TabType.STARS_TO_USER,
            TabType.MENTION_TO_USER ->
                "to ${detailFragment.bookmark.user}"

            TabType.STARS_FROM_USER,
            TabType.MENTION_FROM_USER ->
                "from ${detailFragment.bookmark.user}"
        }

    fun getPageTitleIcon(position: Int) =
        when (tabs[position]) {
            TabType.STARS_TO_USER,
            TabType.STARS_FROM_USER ->
                R.drawable.ic_star


            TabType.MENTION_TO_USER,
            TabType.MENTION_FROM_USER ->
                R.drawable.ic_baseline_chat_bubble
        }

    fun findFragment(viewPager: ViewPager, position: Int) =
        if (position < count) instantiateItem(viewPager, position)
        else null

    /** タブリストが変更されたときに呼ばれるリスナ */
    private var onDataSetChangedListener : Listener<Unit>? = null

    /** タブリストが変更されたときに呼ばれるリスナを設定 */
    fun setOnDataSetChangedListener(listener: Listener<Unit>?) {
        onDataSetChangedListener = listener
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        onDataSetChangedListener?.invoke(Unit)
    }
}
