package com.suihan74.utilities.extensions

import android.webkit.WebView

/**
 * ブランクページに遷移する
 */
fun WebView.loadBlank() = this.loadUrl("about:blank")

/**
 * `Activity`終了時に共通の`WebView`終了前処理を行う
 *
 * リソースの読み込み、スクリプトの実行をすべて中断させる。
 * `Activity`終了時に実行しないと次に`WebView`を開いたときに前回の続きから始まってしまう
 *
 * https://developer.android.com/reference/android/webkit/WebView.html#clearView()
 */
fun WebView.finish() {
    this.stopLoading()
    loadBlank()
}
