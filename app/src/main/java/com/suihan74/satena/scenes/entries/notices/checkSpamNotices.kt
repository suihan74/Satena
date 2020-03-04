package com.suihan74.satena.scenes.entries.notices

import com.suihan74.hatenaLib.Notice

private val spamRegex = Regex("""はてなブックマーク\s*-\s*\d+に関する.+のブックマーク""")

/** スパムからのスターの特徴に当てはまるか確認する */
fun Notice.checkFromSpam() =
    spamRegex.matches(this.metadata?.subjectTitle ?: "")
