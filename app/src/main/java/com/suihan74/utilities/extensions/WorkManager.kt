package com.suihan74.utilities.extensions

import androidx.work.WorkInfo
import androidx.work.WorkManager

/** `tag`をもつ生きている`Worker`が存在するか確認する */
fun WorkManager.checkRunningByTag(tag: String) : Boolean {
    val existedResult = runCatching {
        val workInfo = this.getWorkInfosByTag(tag)
        workInfo.get().any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }
    return existedResult.getOrDefault(false)
}
