package com.suihan74.utilities

import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
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
    val type: RecyclerType,
    var body: T? = null
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

open class LoadableFooterViewHolder(private val view : View) : RecyclerView.ViewHolder(view) {
    fun showProgressBar() {
        view.footer_text.visibility = View.INVISIBLE
        view.footer_progress_bar.visibility = View.VISIBLE
    }

    fun hideProgressBar(nextLoadable: Boolean) {
        view.footer_text.visibility = nextLoadable.toVisibility()
        view.footer_progress_bar.visibility = View.INVISIBLE
    }
}
