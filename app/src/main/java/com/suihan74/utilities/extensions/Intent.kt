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
    val dummyIntent = Intent(this.action, Uri.parse("https://"))

    val intents =
        packageManager.queryIntentActivities(this, PackageManager.MATCH_ALL).plus(
            packageManager.queryIntentActivities(dummyIntent, PackageManager.MATCH_ALL)
        )
        .distinctBy { it.activityInfo.name }
        .filterNot { it.activityInfo.packageName == context.packageName }
        .map { Intent(this).apply {
            setPackage(it.activityInfo.packageName) }
        }

    return when (intents.size) {
        0 -> this
        1 -> intents.first()
        else -> Intent.createChooser(Intent(), title).apply {
            putExtra(Intent.EXTRA_ALTERNATE_INTENTS, intents.toTypedArray())
        }
    }
}

/**
 * デフォルトアプリでURLを開くIntentを作成する
 */
fun Intent.createIntentWithDefaultBrowser(context: Context) : Intent? {
    val packageManager = context.packageManager
    val dummyIntent = Intent(this.action, Uri.parse("https://"))
    val resolveInfo = packageManager.resolveActivity(dummyIntent, PackageManager.MATCH_DEFAULT_ONLY)
        ?: return null

    return Intent(this).apply {
        setPackage(resolveInfo.activityInfo.packageName)
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
