package com.suihan74.utilities.extensions

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/** スクロールの感度（大きい値ほど反応が鈍くなる） */
var ViewPager2.touchSlop : Int
    get() = RecyclerView::class.java.getDeclaredField("mTouchSlop").let { field ->
        val recyclerView = ViewPager2::class.java.getDeclaredField("mRecyclerView").let { recyclerViewField ->
            recyclerViewField.isAccessible = true
            recyclerViewField.get(this) as RecyclerView
        }
        field.isAccessible = true
        field.get(recyclerView) as Int
    }
    set(value) {
        RecyclerView::class.java.getDeclaredField("mTouchSlop").let { field ->
            val recyclerView = ViewPager2::class.java.getDeclaredField("mRecyclerView").let { recyclerViewField ->
                recyclerViewField.isAccessible = true
                recyclerViewField.get(this) as RecyclerView
            }
            field.isAccessible = true
            field.set(recyclerView, value)
        }
    }
