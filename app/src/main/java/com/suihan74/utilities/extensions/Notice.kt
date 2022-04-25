package com.suihan74.utilities.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R

private val spamRegex by lazy { Regex("""はてなブックマーク\s*-\s*\d+に関する.+のブックマーク""") }

/** スパムからのスターの特徴に当てはまるか確認する */
fun Notice.checkFromSpam() =
    spamRegex.matches(this.metadata?.subjectTitle.orEmpty())

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
    val comment = metadata?.subjectTitle.orEmpty().toCharArray()
    val sourceComment = comment.joinToString(
        separator = "",
        limit = 9,
        truncated = "..."
    )

    val nameColor = ContextCompat.getColor(context, R.color.colorPrimary)
    val users = this.users
    val usersStr = this.users.joinToString(
        separator = "、",
        limit = 3,
        truncated = "ほか${users.count() - 3}人",
        transform = { "<font color=\"$nameColor\">${it}</font>さん" }
    )

    return when (verb) {
        Notice.VERB_STAR -> {
            val starsText =
                this.objects.distinctBy { it.color }
                    .reversed()
                    .joinToString(separator = "") {
                        val starColor = ContextCompat.getColor(context, it.color.colorId)
                        "<font color=\"${starColor}\">★</font>"
                    }

            if (this.link.startsWith("https://b.hatena.ne.jp/")) {
                "${usersStr}があなたのブコメ($sourceComment)に${starsText}をつけました"
            }
            else {
                "${usersStr}があなたの($sourceComment)に${starsText}をつけました"
            }
        }

        Notice.VERB_ADD_FAVORITE ->
            "${usersStr}があなたのブックマークをお気に入りに追加しました"

        Notice.VERB_BOOKMARK ->
            "${usersStr}があなたのエントリをブックマークしました"

        Notice.VERB_FIRST_BOOKMARK -> {
            runCatching {
                val md = metadata!!.firstBookmarkMetadata!!
                val titleDigest =
                    md.entryTitle.toCharArray().joinToString(
                        separator = "",
                        limit = 9,
                        truncated = "..."
                    )
                "1番目にブクマした記事が${md.totalBookmarksAchievement}usersに達しました (${titleDigest})"
            }.getOrElse {
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("failed to reference firstBookmarkMetadata"))
                "1番目にブクマした記事が注目されています"
            }
        }

        else ->
            "[sorry, not implemented notice] users: $usersStr , verb: ${this.verb}"
    }
}
