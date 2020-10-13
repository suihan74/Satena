package com.suihan74.satena.scenes.bookmarks2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.suihan74.hatenaLib.StarColor
import com.suihan74.hatenaLib.UserColorStarsCount
import com.suihan74.satena.R
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.dp2px
import com.suihan74.utilities.extensions.sp2px
import kotlinx.android.synthetic.main.popup_add_star.view.*

class AddStarPopupMenu(context: Context) : PopupWindow() {
    init {
        val view = LayoutInflater.from(context).inflate(
            R.layout.popup_add_star,
            null
        ).apply {
            add_blue_star.setOnClickListener {
                onClickButton(StarColor.Blue, colorStars.blue)
            }

            add_red_star.setOnClickListener {
                onClickButton(StarColor.Red, colorStars.red)
            }

            add_green_star.setOnClickListener {
                onClickButton(StarColor.Green, colorStars.green)
            }

            add_yellow_star.setOnClickListener {
                onClickButton(StarColor.Yellow, 1)
            }
        }

        contentView = view
        width = context.dp2px(18 * 4 + 24 * 2 + 24 * 3)
        height = context.dp2px(18 * 2 + 18) + context.sp2px(13.5f)
        isFocusable = true
        isTouchable = true
        elevation = context.dp2px(8).toFloat()
    }

    /** ユーザーが所持しているカラースター */
    private var colorStars: UserColorStarsCount = UserColorStarsCount(0, 0, 0, 0)

    /** スターをつけるボタンをクリックしたときの挙動 */
    private var onClickAddStarListener : Listener<StarColor>? = null

    /** スターを購入するボタンをクリックしたときの挙動 */
    private var onClickPurchaseStarsListener : Listener<Unit>? = null

    /** スターをつけるボタンをクリックしたときの挙動を設定する */
    fun setOnClickAddStarListener(listener: Listener<StarColor>?) {
        onClickAddStarListener = listener
    }

    /** スターを購入するボタンをクリックしたときの挙動 */
    fun setOnClickPurchaseStarsListener(listener: Listener<Unit>?) {
        onClickPurchaseStarsListener = listener
    }

    /** ユーザーが所持しているスター数を監視する */
    fun observeUserStars(
        lifecycleOwner: LifecycleOwner,
        liveData: LiveData<UserColorStarsCount>?
    ) {
        liveData?.observe(lifecycleOwner) {
            colorStars = it ?: UserColorStarsCount(0, 0, 0, 0)
            initializeView(contentView.add_blue_star, contentView.stars_count_blue, colorStars.blue)
            initializeView(contentView.add_red_star, contentView.stars_count_red, colorStars.red)
            initializeView(contentView.add_green_star, contentView.stars_count_green, colorStars.green)
        }
    }

    /** 所持カラースター個数によってスターを付けるか購入画面に遷移するか切り替える */
    private fun onClickButton(color: StarColor, count: Int) {
        if (count > 0) {
            onClickAddStarListener?.invoke(color)
        }
        else {
            onClickPurchaseStarsListener?.invoke(Unit)
        }
    }

    /** ボタンと所持スター数の表示を設定する */
    private fun initializeView(button: ImageButton, textView: TextView, count: Int) {
        if (count > 0) {
            button.setImageResource(R.drawable.ic_add_star_filled)
            TooltipCompat.setTooltipText(button, "")
            textView.text = count.toString()
            textView.visibility = View.VISIBLE
        }
        else {
            button.setImageResource(R.drawable.ic_add_shopping_cart)
            TooltipCompat.setTooltipText(button, "カラースター購入ページを表示します")
            textView.text = ""
            textView.visibility = View.GONE
        }
    }
}
