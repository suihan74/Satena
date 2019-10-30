package com.suihan74.utilities

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewScrollingUpdater(
    private val adapter: RecyclerView.Adapter<*>
) : RecyclerView.OnScrollListener() {

    var isLoading : Boolean = false
        private set

    private val invokingPosition
        get() = adapter.itemCount - 1

    final override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = recyclerView.childCount
        val manager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItem = manager.findFirstVisibleItemPosition()
        val lastInScreen = firstVisibleItem + visibleItemCount

        if (checkInvoking(lastInScreen)) {
            invokeLoading()
        }
    }

    private fun checkInvoking(lastInScreen: Int) =
        invokingPosition != 0 && !isLoading && invokingPosition <= lastInScreen

    protected abstract fun load()

    fun invokeLoading() {
        if (!isLoading) {
            isLoading = true
            load()
        }
    }

    protected fun loadCompleted() {
        isLoading = false
    }
}
