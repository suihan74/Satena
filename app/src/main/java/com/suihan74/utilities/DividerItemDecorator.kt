package com.suihan74.utilities

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView

// RecyclerViewのアイテム間に区切り線を描画する
// ただし最後の項目(主にFooter)の下端には描画しないようにする(Viewが画面より小さい場合無駄な区切り線が表示されてダサいため)
class DividerItemDecorator(private val divider : Drawable) : RecyclerView.ItemDecoration() {
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val divLeft = parent.paddingLeft
        val divRight = parent.width - parent.paddingRight

        val bodiesCount = parent.childCount - 2
        for (i in 0..bodiesCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val divTop = child.bottom + params.bottomMargin
            val divBottom = divTop + divider.intrinsicHeight

            divider.setBounds(divLeft, divTop, divRight, divBottom)
            divider.draw(c)
        }
    }
}
