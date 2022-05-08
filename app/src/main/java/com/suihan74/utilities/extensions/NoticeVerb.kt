package com.suihan74.utilities.extensions

import com.suihan74.hatenaLib.NoticeVerb
import com.suihan74.satena.R

object NoticeVerbCompat {
    fun valueTextPairs() : List<Pair<NoticeVerb, Int>> =
        listOf(
            NoticeVerb.ADD_FAVORITE to R.string.notice_verb_add_favorite,
            NoticeVerb.STAR to R.string.notice_verb_star,
            NoticeVerb.BOOKMARK to R.string.notice_verb_bookmark,
            NoticeVerb.FIRST_BOOKMARK to R.string.notice_verb_first_bookmark,
            NoticeVerb.OTHERS to R.string.notice_verb_others
        )
}
