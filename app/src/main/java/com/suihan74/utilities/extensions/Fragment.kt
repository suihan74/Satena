package com.suihan74.utilities.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment

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
