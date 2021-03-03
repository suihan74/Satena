package com.suihan74.utilities

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.suihan74.satena.databinding.FooterRecyclerViewLoadableBinding

// RecyclerViewでヘッダ・フッタ・セクションを使用するために必要なものたち

enum class RecyclerType(val id: Int) {
    HEADER(0),
    FOOTER(1),
    SECTION(2),
    BODY(3);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: BODY
    }
}

data class RecyclerState<T>(
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

open class HeaderViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
open class FooterViewHolder(val binding : ViewBinding) : RecyclerView.ViewHolder(binding.root)
open class SectionViewHolder(val binding : ViewBinding) : RecyclerView.ViewHolder(binding.root)

open class LoadableFooterViewHolder(
    val binding : FooterRecyclerViewLoadableBinding
) : RecyclerView.ViewHolder(binding.root) {
}
