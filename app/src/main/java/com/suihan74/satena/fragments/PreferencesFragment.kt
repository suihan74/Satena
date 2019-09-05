package com.suihan74.satena.fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.suihan74.utilities.BackPressable
import com.suihan74.satena.adapters.tabs.PreferencesTabAdapter
import com.suihan74.satena.adapters.tabs.PreferencesTabMode
import com.suihan74.satena.R
import com.suihan74.utilities.PermissionRequestable

class PreferencesFragment : Fragment(), BackPressable, PermissionRequestable {
    private lateinit var mRoot : View
    private lateinit var mViewPager : ViewPager

    private lateinit var mTabAdapter : PreferencesTabAdapter
    private var mThemeChanged: Boolean = false

    companion object {
        fun createInstance(themeChanged: Boolean = false) : PreferencesFragment = PreferencesFragment().apply {
            mThemeChanged = themeChanged
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences, container, false)
        mRoot = root

        val toolbar = root.findViewById<Toolbar>(R.id.preferences_toolbar)

        mTabAdapter = PreferencesTabAdapter(childFragmentManager)
        mViewPager = root.findViewById<ViewPager>(R.id.preferences_view_pager).apply {
            // 環状スクロールできるように細工
            adapter = mTabAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                private var jumpPosition = -1

                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager.SCROLL_STATE_IDLE && jumpPosition > 0) {
                        mViewPager.setCurrentItem(jumpPosition, false)
                        jumpPosition = -1
                    }
                }

                // position & jumpPosition => 1 ~ actualCount  // head&tailを含むインデックス
                // fixedPosition => 0 ~ actualCount-1  // head&tailを無視したコンテンツのインデックス(0: ACCOUNTS, 1: GENERALS, ...)
                override fun onPageSelected(position: Int) {
                    for (i in 0 until mTabAdapter.getActualCount()) {
                        val btn = mRoot.findViewById<ImageButton>(mTabAdapter.getIconId(i))
                        btn.setBackgroundColor(Color.TRANSPARENT)
                    }

                    when (position) {
                        PreferencesTabMode.DUMMY_HEAD.int -> { jumpPosition = mTabAdapter.getActualCount() }
                        PreferencesTabMode.DUMMY_TAIL.int -> { jumpPosition = 1 }
                    }

                    val fixedPosition = (if (jumpPosition > 0) jumpPosition else position) - 1
                    val btn = mRoot.findViewById<ImageButton>(mTabAdapter.getIconId(fixedPosition))
                    btn.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
                    toolbar.title = "設定 > ${getString(mTabAdapter.getPageTitleId(fixedPosition))}"
                }
            })
        }

        val tab = activity!!.intent.extras?.getSerializable("current_tab") as? PreferencesTabMode ?: PreferencesTabMode.INFORMATION
        val position = tab.int
        mViewPager.setCurrentItem(position, false)
        toolbar.title = "設定 > ${getString(tab.titleId)}"

        retainInstance = true
        return root
    }

    fun onClickedTab(view: View) {
        mViewPager.currentItem = mTabAdapter.getIndexFromIconId(view.id)
    }

    override fun onBackPressed(): Boolean {
        val currentFragment = mTabAdapter.findFragment(mViewPager, mViewPager.currentItem)
        return if (currentFragment is BackPressable) {
            currentFragment.onBackPressed()
        }
        else {
            false
        }
    }

    override fun onRequestPermissionsResult(pairs: List<Pair<String, Int>>) {
        val currentFragment = mTabAdapter.findFragment(mViewPager, mViewPager.currentItem)
        if (currentFragment is PermissionRequestable) {
            currentFragment.onRequestPermissionsResult(pairs)
        }
    }
}
