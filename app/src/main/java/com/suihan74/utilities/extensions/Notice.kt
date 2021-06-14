package com.suihan74.utilities.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R

private val spamRegex by lazy { Regex("""はてなブックマーク\s*-\s*\d+に関する.+のブックマーク""") }

/** スパムからのスターの特徴に当てはまるか確認する */
fun Notice.checkFromSpam() =
    spamRegex.matches(this.metadata?.subjectTitle ?: "")

/**
 * 通知に含まれるユーザー名を抽出する
 */
val Notice.users get() =
    objects
    .groupBy { it.user }
    .mapNotNull { it.value.firstOrNull() }
    .reversed()
    .map { it.user }

/**
 * 通知メッセージを作成する
 */
fun Notice.message(context: Context) : String {
    val nameColor = ContextCompat.getColor(context, R.color.colorPrimary)

    val comment = (metadata?.subjectTitle ?: "").toCharArray()
    val sourceComment = comment.joinToString(
        separator = "",
        limit = 9,
        truncated = "..."
    )

    val users = this.users
    val usersStr = this.users
        .joinToString(
            separator = "、",
            limit = 3,
            truncated = "ほか${users.count() - 3}人",
            transform = { "<font color=\"$nameColor\">${it}</font>さん" })

    return when (verb) {
        Notice.VERB_STAR -> {
            val starColor = ContextCompat.getColor(context, R.color.starYellow)
            if (link.startsWith("https://b.hatena.ne.jp/")) {
                "${usersStr}があなたのブコメ($sourceComment)に<font color=\"$starColor\">★</font>をつけました"
            }
            else {
                "${usersStr}があなたの($sourceComment)に<font color=\"$starColor\">★</font>をつけました"
            }
        }

        Notice.VERB_ADD_FAVORITE ->
            "${usersStr}があなたのブックマークをお気に入りに追加しました"

        Notice.VERB_BOOKMARK ->
            "${usersStr}があなたのエントリをブックマークしました"

        else ->
            "[sorry, not implemented notice] users: $usersStr , verb: $verb"
    }
}
