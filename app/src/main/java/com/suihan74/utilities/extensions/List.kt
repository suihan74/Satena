package com.suihan74.utilities.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 並行map
 */
suspend fun <T, R> Iterable<T>.parallelMap(transform: suspend (T)->R): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}

// ------ //

/**
 * 最初に見つかる同一の項目を新しいvalueで更新するか、項目が未だ無い場合は新たに追加する
 *
 * 追加する場合、リストの末尾に追加する
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

/**
 * 最初に見つかる同一の項目を新しいvalueで更新するか、項目が未だ無い場合は新たに追加する
 *
 * 追加する場合、リストの先頭に追加する
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <T> List<T>.updateFirstOrPlusAhead(value: T, predicate: (T)->Boolean) : List<T> {
    val prevList = this
    val idx = prevList.indexOfFirst(predicate)

    return if (idx < 0) listOf(value).plus(prevList)
    else buildList(this.size + 1) {
        addAll(prevList.subList(0, idx))
        add(value)
        addAll(prevList.subList(idx + 1, prevList.size))
    }
}

// ------ //

/** Collectionが空のときだけactionを実行する */
inline fun <T> Collection<T>?.onEmpty(crossinline action: (Collection<T>)->Unit) {
    if (this != null && this.isEmpty()) {
        action.invoke(this)
    }
}

/** Collectionがnullか空のときだけactionを実行する */
inline fun <T> Collection<T>?.onNullOrEmpty(crossinline action: (Collection<T>?)->Unit) {
    if (this.isNullOrEmpty()) {
        action.invoke(this)
    }
}

/** Collectionが空ではないときだけactionを実行する */
inline fun <T> Collection<T>?.onNotEmpty(crossinline action: (Collection<T>)->Unit) {
    if (!this.isNullOrEmpty()) {
        action.invoke(this)
    }
}
