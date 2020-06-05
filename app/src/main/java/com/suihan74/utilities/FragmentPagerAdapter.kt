package com.suihan74.utilities

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter

/** 全てのタブに対して処理を行う */
@Suppress("UNCHECKED_CAST")
fun <T : Fragment> FragmentPagerAdapter.map(container: ViewGroup, action: (T)->Unit) {
    (0 until count).forEach { idx ->
        (instantiateItem(container, idx) as? T)?.let { instance ->
            action(instance)
        }
    }
}
