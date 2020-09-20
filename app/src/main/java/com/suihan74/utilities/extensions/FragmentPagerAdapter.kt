package com.suihan74.utilities.extensions

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter

/** 全てのタブに対して処理を行う */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Fragment> FragmentPagerAdapter.map(
    container: ViewGroup,
    crossinline action: (T)->Unit
) {
    repeat (count) { i ->
        instantiateItem(container, i).alsoAs<T> { instance ->
            action(instance)
        }
    }
}