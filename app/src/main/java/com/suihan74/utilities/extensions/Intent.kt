package com.suihan74.utilities.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * URLを開くために"共有先リストからこのアプリを除いた"Intentを作成する
 */
fun Intent.createIntentWithoutThisApplication(context: Context, title: CharSequence = "Choose a browser") : Intent {
    val packageManager = context.packageManager
    val dummyIntent = Intent(this.action, Uri.parse("https://dummy"))

    val intents =
        packageManager.queryIntentActivities(dummyIntent, PackageManager.MATCH_ALL)
        .filterNot { it.activityInfo.packageName == context.packageName }
        .map { Intent(this).apply { setPackage(it.activityInfo.packageName) } }

    return when (intents.size) {
        0 -> this
        1 -> intents.first()
        else -> Intent.createChooser(this, title).apply {
            putExtra(Intent.EXTRA_ALTERNATE_INTENTS, intents.toTypedArray())
        }
    }
}

/**
 * "intent://"スキームなどで外部からのアクティビティ遷移指定を処理する際のセキュリティ対策を行う
 *
 * 外部に公開していないアクティビティを開けないようにする
 */
fun Intent.withSafety() : Intent = this.also {
    it.addCategory(Intent.CATEGORY_BROWSABLE)
    it.component = null
    it.selector?.let { selector ->
        selector.addCategory(Intent.CATEGORY_BROWSABLE)
        selector.component = null
    }
}
