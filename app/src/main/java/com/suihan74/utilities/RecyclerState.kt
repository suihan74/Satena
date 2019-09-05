package com.suihan74.utilities

import android.support.v7.widget.RecyclerView
import android.view.View

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

class RecyclerState<T>(val type: RecyclerType, var body: T? = null) {
    companion object {
        fun <T> makeStatesWithFooter(datas : List<T>) : ArrayList<RecyclerState<T>> {
            val list = ArrayList<RecyclerState<T>>()
            datas.forEach { list.add(RecyclerState(RecyclerType.BODY, it)) }
            list.add(RecyclerState(RecyclerType.FOOTER))

            return list
        }
    }
}

open class HeaderViewHolder(view : View) : RecyclerView.ViewHolder(view)
open class FooterViewHolder(view : View) : RecyclerView.ViewHolder(view)
open class SectionViewHolder(view : View) : RecyclerView.ViewHolder(view)
