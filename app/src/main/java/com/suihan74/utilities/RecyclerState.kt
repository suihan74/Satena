package com.suihan74.utilities

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.footer_recycler_view_loadable.view.*

// RecyclerViewでヘッダ・フッタ・セクションを使用するために必要なものたち

enum class RecyclerType(val int: Int) {
    HEADER(0),
    FOOTER(1),
    SECTION(2),
    BODY(3);

    companion object {
        fun fromInt(i: Int) : RecyclerType =
            values().firstOrNull { it.int == i } ?: BODY
    }
}

class RecyclerState<T>(
    /** アイテムの種類 */
    val type: RecyclerType,

    /** RecyclerType.BODYの場合のアイテムデータ */
    var body: T? = null,

    /** HEADER, FOOTER, SECTION用の追加情報 */
    var extra: Any? = null
) {
    companion object {
        fun <T> makeStatesWithFooter(src : List<T>) : ArrayList<RecyclerState<T>> =
            ArrayList<RecyclerState<T>>(src.size + 1).apply {
                addAll(src.map { RecyclerState(RecyclerType.BODY, it) })
                add(RecyclerState(RecyclerType.FOOTER))
            }

        fun bodiesCount(states: List<RecyclerState<*>>) =
            states.count { RecyclerType.BODY == it.type }

        fun isBodiesEmpty(states: List<RecyclerState<*>>) =
            states.none { RecyclerType.BODY == it.type }
    }
}

open class HeaderViewHolder(view : View) : RecyclerView.ViewHolder(view)
open class FooterViewHolder(view : View) : RecyclerView.ViewHolder(view)
open class SectionViewHolder(view : View) : RecyclerView.ViewHolder(view)

open class LoadableFooterViewHolder(
    private val view : View
) : RecyclerView.ViewHolder(view) {

    /** 「追加更新する」ボタン */
    val additionalLoadingTextView : TextView?
        get() = view.footer_text

    /** 更新中表示 */
    val progressBar: ProgressBar?
        get() = view.footer_progress_bar

    fun showProgressBar() {
        additionalLoadingTextView?.visibility = View.GONE
        view.footer_progress_bar.visibility = View.VISIBLE
    }

    fun hideProgressBar(nextLoadable: Boolean) {
//        additionalLoadingTextView?.visibility = nextLoadable.toVisibility()
        view.footer_progress_bar.visibility = View.INVISIBLE
    }
}
