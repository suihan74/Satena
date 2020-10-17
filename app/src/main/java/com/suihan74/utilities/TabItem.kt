package com.suihan74.utilities

/**
 * タブの表示項目として扱える
 */
interface TabItem {

    /** タブが選択された */
    fun onTabSelected()

    /** タブの選択が解除された*/
    fun onTabUnselected()

    /** タブが再選択された */
    fun onTabReselected()
}
