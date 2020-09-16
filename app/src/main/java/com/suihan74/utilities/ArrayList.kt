@file:Suppress("unused")

package com.suihan74.utilities

/**
 * valueがリストに存在しないなら追加する
 *
 * @param value 追加する値
 */
fun <T> ArrayList<T>.addUnique(value: T) {
    synchronized(this) {
        if (!this.contains(value)) {
            this.add(value)
        }
    }
}

/**
 * valueがリストに存在しないなら追加する
 *
 * @param value 追加する値
 * @param selector 比較に使用するプロパティを選択する
 */
fun <T, U> ArrayList<T>.addUnique(value: T, selector: (T)->U) {
    synchronized(this) {
        val v = selector(value)
        if (this.none { selector(it) == v }) {
            this.add(value)
        }
    }
}
