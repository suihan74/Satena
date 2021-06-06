package com.suihan74.satena.scenes.post

import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

enum class TagsListOrder(override val textId: Int) : TextIdContainer {
    /** インデクス順 (50音順) */
    INDEX(R.string.post_bookmark_tags_list_order_index),

    /** 使用回数順 */
    COUNT(R.string.post_bookmark_tags_list_order_count)
    ;

    companion object {
        fun fromOrdinal(i: Int) = values().getOrElse(i) { INDEX }
    }
}
