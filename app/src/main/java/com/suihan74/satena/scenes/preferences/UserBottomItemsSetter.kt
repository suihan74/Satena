package com.suihan74.satena.scenes.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuItemCompat
import androidx.databinding.BindingAdapter
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.utilities.getThemeColor
import kotlinx.android.synthetic.main.view_user_bottom_items_setter.view.*

class UserBottomItemsSetter : CoordinatorLayout {
    companion object {
        @JvmStatic
        @BindingAdapter("items")
        fun setItems(instance: UserBottomItemsSetter, items: List<UserBottomItem>?) {
            instance.items = items ?: emptyList()
            instance.inflateButtons()
        }
    }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleInt: Int
    ) : super(context, attrs, defStyleInt) {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_user_bottom_items_setter, this, true)
    }

    /** 表示する項目 */
    private var items: List<UserBottomItem> = emptyList()

    private fun inflateButtons() {
        val bottomAppBar = this.bottom_app_bar
        val tint = context.getThemeColor(R.attr.textColor)
        items.forEach {
            bottomAppBar.menu.add(it.textId).apply {
                setIcon(it.iconId)
                MenuItemCompat.setIconTintList(
                    this,
                    ColorStateList.valueOf(tint)
                )
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
    }
}
