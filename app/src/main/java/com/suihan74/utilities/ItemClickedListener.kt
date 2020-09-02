package com.suihan74.utilities

/** リスト項目クリック時のイベントリスナ */
typealias ItemClickedListener<T> = (T)->Unit

/** リスト項目連続複数回クリック時のイベントリスナ */
typealias ItemMultipleClickedListener<T> = (T, Int)->Unit

/** リスト項目長押し時のイベントリスナ */
typealias ItemLongClickedListener<T> = (T)->Boolean
