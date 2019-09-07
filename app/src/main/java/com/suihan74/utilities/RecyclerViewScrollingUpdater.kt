package com.suihan74.utilities

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewScrollingUpdater(private var invokingPosition: Int) : RecyclerView.OnScrollListener() {
    private var isLoading : Boolean = false

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
        invokingPosition != 0 && !isLoading && invokingPosition == lastInScreen

    abstract fun load()

    protected fun loadCompleted(newInvokingPosition: Int = invokingPosition) {
        isLoading = false
        refreshInvokingPosition(newInvokingPosition)
    }

    fun refreshInvokingPosition(newInvokingPosition: Int) {
        invokingPosition = newInvokingPosition
    }
}
