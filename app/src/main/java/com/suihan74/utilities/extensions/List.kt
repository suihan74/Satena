package com.suihan74.utilities.extensions

/**
 * 最初に見つかる同一の項目を新しいvalueで更新するか、項目が未だ無い場合は新たに追加する
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <T> List<T>.updateFirstOrPlus(value: T, predicate: (T)->Boolean) : List<T> {
    val prevList = this
    val idx = prevList.indexOfFirst(predicate)

    return if (idx < 0) prevList.plus(value)
    else buildList(this.size + 1) {
        addAll(prevList.subList(0, idx))
        add(value)
        addAll(prevList.subList(idx + 1, prevList.size))
    }
}
