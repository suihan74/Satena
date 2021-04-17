package com.suihan74.utilities.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * URLを開くために"共有先リストからこのアプリを除いた"Intentを作成する
 */
fun Intent.createIntentWithoutThisApplication(context: Context, title: CharSequence = "Choose a browser") : Intent {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        return Intent.createChooser(this, title).apply {
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(context.packageName))
        }
    }
    else {
        val packageManager = context.packageManager
        val dummyIntent = Intent(this.action, Uri.parse("https://dummy"))
        val intentActivities =
            packageManager.queryIntentActivities(dummyIntent, PackageManager.MATCH_ALL)
                .plus(packageManager.queryIntentActivities(this, PackageManager.MATCH_ALL))
                .distinctBy { it.activityInfo.name }

        val intents = intentActivities
            .filterNot { it.activityInfo.packageName == context.packageName }
            .map { Intent(this).apply { setPackage(it.activityInfo.packageName) } }

        return when (intents.size) {
            0 -> this
            1 -> intents.first()
            else -> Intent.createChooser(Intent(), title).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }
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
