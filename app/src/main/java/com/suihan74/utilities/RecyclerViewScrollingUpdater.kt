package com.suihan74.utilities

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@Suppress("unused")
class RecyclerViewScrollingUpdater(
    /** ロード開始処理 */
    private var onLoading: (RecyclerViewScrollingUpdater.()->Unit)? = null
) : RecyclerView.OnScrollListener() {

    /** ロード開始処理終了時に成功失敗に関わらず呼び出す */
    private var onFinally : (RecyclerViewScrollingUpdater.(Throwable?)->Unit)? = null

    /** ロード開始失敗時 */
    private var onError : (RecyclerViewScrollingUpdater.(Throwable)->Unit)? = null

    /** ロード開始成功時 */
    private var onSuccess : (RecyclerViewScrollingUpdater.()->Unit)? = null

    /** ロード開始処理を設定 */
    fun setOnLoadingListener(listener: (RecyclerViewScrollingUpdater.()->Unit)?) {
        onLoading = listener
    }

    /** ロード開始処理を設定 */
    fun setOnFinallyListener(listener: (RecyclerViewScrollingUpdater.(Throwable?)->Unit)?) {
        onFinally = listener
    }

    /** ロード開始失敗時処理を設定 */
    fun setOnErrorListener(listener: (RecyclerViewScrollingUpdater.(Throwable)->Unit)?) {
        onError = listener
    }

    /** ロード開始成功時処理を設定 */
    fun setOnCompletedListener(listener: (RecyclerViewScrollingUpdater.()->Unit)?) {
        onSuccess = listener
    }

    /** ロード処理実行中か否かを表す */
    var isLoading : Boolean = false
        get() = synchronized(this) { field }
        private set(value) {
            synchronized(this) {
                field = value
            }
        }

    /**
     * ロード完了時に手動で必ず呼ぶ
     *
     * これが呼ばれないと次以降の更新処理は実行されないので注意が必要
     */
    fun loadCompleted() {
        isLoading = false
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val invokingPosition = recyclerView.adapter!!.itemCount - 1
        val visibleItemCount = recyclerView.childCount
        val manager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItem = manager.findFirstVisibleItemPosition()
        val lastInScreen = firstVisibleItem + visibleItemCount

        if (dy != 0 && checkInvoking(invokingPosition, lastInScreen)) {
            invokeLoading()
        }
    }

    private fun checkInvoking(invokingPosition: Int, lastInScreen: Int) =
        invokingPosition > 0 && !isLoading && invokingPosition <= lastInScreen

    private fun load() {
        var error : Throwable? = null
        try {
            onLoading?.invoke(this)
        }
        catch (e: Throwable) {
            error = e
            onError?.invoke(this, e)
        }
        finally {
            if (error != null) {
                onSuccess?.invoke(this)
            }
            onFinally?.invoke(this, error)
            if (error != null) {
                loadCompleted()
            }
        }
    }

    private fun invokeLoading() {
        if (!isLoading) {
            isLoading = true
            load()
        }
    }
}
