package com.suihan74.utilities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.satena.R

/**
 * スワイプで展開状態を変更できる`BottomSheetDialogFragment`
 */
abstract class ExpandableBottomSheetDialogFragment : BottomSheetDialogFragment() {
    /**
     * コンパクト表示時に隠す領域一番上にあるView
     *
     * `onViewCreated`が呼ばれるタイミングでの値で内容が確定する点に注意
     * nullに設定することで通常の`BottomSheetDialogFragment`として振舞う
     */
    open val hiddenTopView : View? = null

    /**
     * デフォルトで最大展開状態にするか
     *
     * `onViewCreated`が呼ばれるタイミングでの値で内容が確定する点に注意
     */
    open val expandBottomSheetByDefault : Boolean = false

    // ------ //

    /** コンパクト表示を有効にする */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val hiddenTopView = hiddenTopView ?: return
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheetInternal = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheetInternal?.updateLayoutParams {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            view.post {
                val parent = view.parent as View
                val params = parent.layoutParams as CoordinatorLayout.LayoutParams
                val behavior = params.behavior as BottomSheetBehavior
                // コンパクト表示中に表示する高さ(最上端からeditText部分まで)
                behavior.peekHeight = hiddenTopView.y.toInt()
                // デフォルトで最大展開する
                if (expandBottomSheetByDefault) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) = this@ExpandableBottomSheetDialogFragment.onSlide(bottomSheet, slideOffset)
                    override fun onStateChanged(bottomSheet: View, newState: Int) = this@ExpandableBottomSheetDialogFragment.onStateChanged(bottomSheet, newState)
                })
            }
        }
    }

    // ------ //

    /** スライド途中のイベントリスナ */
    open fun onSlide(bottomSheet: View, slideOffset: Float) {}

    /** 状態が変化した際のイベントリスナ */
    open fun onStateChanged(bottomSheet: View, newState: Int) {}
}
