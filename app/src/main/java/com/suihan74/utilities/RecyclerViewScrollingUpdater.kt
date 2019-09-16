package com.suihan74.utilities

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewScrollingUpdater(
    private val adapter: RecyclerView.Adapter<*>
) : RecyclerView.OnScrollListener() {

    private var isLoading : Boolean = false

    private val invokingPosition
        get() = adapter.itemCount - 1

    final override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = recyclerView.childCount
        val manager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItem = manager.findFirstVisibleItemPosition()
        val lastInScreen = firstVisibleItem + visibleItemCount

        if (checkInvoking(lastInScreen)) {
            isLoading = true
            load()
        }
    }

    private fun checkInvoking(lastInScreen: Int) =
        invokingPosition != 0 && !isLoading && invokingPosition <= lastInScreen

    abstract fun load()

    protected fun loadCompleted() {
        isLoading = false
    }
}
