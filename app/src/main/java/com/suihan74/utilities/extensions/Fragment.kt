package com.suihan74.utilities.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Fragment生成時にargumentsを渡す用途で使用する
 *
 * example -> HogeFragment().withArguments { putInt(ARG_NUM, 1) }
 */
@Suppress("unused")
inline fun <reified T : Fragment> T.withArguments(initializer: (Bundle.(T)->Unit)) : T {
    val arguments = this.arguments ?: Bundle()
    this.arguments = arguments
    initializer.invoke(arguments, this)
    return this
}

/**
 * Fragment生成時にargumentsを渡す用途で使用する
 *
 * example -> HogeFragment().withArguments { putInt(ARG_NUM, 1) }
 */
@Suppress("unused")
inline fun <reified T : Fragment> T.withArguments() : T {
    val arguments = this.arguments ?: Bundle()
    this.arguments = arguments
    return this
}

// ------ //

/** 型を指定してアクティビティを取得する */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : FragmentActivity> Fragment.requireActivity() : T {
    return requireActivity() as T
}

/** 型を指定して親フラグメントを取得する */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Fragment> Fragment.requireParentFragment() : T {
    return requireParentFragment() as T
}
