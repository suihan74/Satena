package com.suihan74.utilities.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/** URLを開くために"共有先リストからこのアプリを除いた"Intentを作成する */
fun Intent.createIntentWithoutThisApplication(context: Context) : Intent {
    val packageManager = context.packageManager
    val dummyIntent = Intent(this.action, Uri.parse("https://dummy"))
    val intentActivities = packageManager.queryIntentActivities(dummyIntent, PackageManager.MATCH_ALL)
        .plus(packageManager.queryIntentActivities(this, PackageManager.MATCH_ALL))
        .distinctBy { it.activityInfo.name }

    val intents = intentActivities
        .filterNot { it.activityInfo.packageName == context.packageName }
        .map { Intent(this).apply { setPackage(it.activityInfo.packageName) } }

    return when (intents.size) {
        0 -> this
        1 -> intents.first()
        else -> Intent.createChooser(Intent(), "Choose a browser").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
        }
    }
}
