package com.suihan74.utilities

import androidx.annotation.MainThread

/**
 * 画面が持つDrawerLayoutを外部から制御する可能性がある
 */
interface DrawerOwner {
    /** 外部からドロワを開く */
    @MainThread
    fun openDrawer()

    /** 外部からドロワを閉じる */
    @MainThread
    fun closeDrawer()
}
