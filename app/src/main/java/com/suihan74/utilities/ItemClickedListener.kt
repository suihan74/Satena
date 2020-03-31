package com.suihan74.utilities

/** リスト項目クリック時のイベントリスナ */
typealias ItemClickedListener<T> = (T)->Unit

/** リスト項目長押し時のイベントリスナ */
typealias ItemLongClickedListener<T> = (T)->Boolean
