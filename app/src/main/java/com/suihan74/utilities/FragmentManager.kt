package com.suihan74.utilities

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

inline fun <reified T> FragmentManager.get() : T? =
    this.fragments.lastOrNull { it is T } as? T

inline fun <reified T> FragmentManager.get(predicate: (Fragment) -> Boolean) : T? =
    this.fragments.lastOrNull(predicate) as? T

/** 最後に実行されたBackStackEntryを取得する */
inline val FragmentManager.topBackStackEntry: FragmentManager.BackStackEntry?
    get() =
        if (backStackEntryCount > 0) getBackStackEntryAt(backStackEntryCount - 1)
        else null
