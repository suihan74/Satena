package com.suihan74.satena.scenes.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuItemCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.utilities.getThemeColor
import kotlinx.android.synthetic.main.view_user_bottom_items_setter.view.*

class UserBottomItemsSetter : CoordinatorLayout {
    companion object {
        @JvmStatic
        @BindingAdapter("items")
        fun setItems(instance: UserBottomItemsSetter, liveData: MutableLiveData<List<UserBottomItem>>?) {
            instance.itemsLiveData = liveData
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
    private var itemsLiveData : MutableLiveData<List<UserBottomItem>>? = null
    private val items: List<UserBottomItem>
        get() = itemsLiveData?.value ?: emptyList()

    /** 表示できるボタンの最大数 */
    private val maxButtonsNum : Int by lazy {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density

        val screenWidthPx = displayMetrics.widthPixels
        val rightMargin = (96 * density).toInt()
        val buttonWidthPx = (48 * density).toInt()

        val numReserved = 2

        (screenWidthPx - rightMargin) / buttonWidthPx - numReserved
    }

    /** 設定用のBottomAppBarにボタンを表示する */
    private fun inflateButtons() {
        val items = items.take(maxButtonsNum)

        val bottomAppBar = this.bottom_app_bar
        val tint = ColorStateList.valueOf(context.getThemeColor(R.attr.textColor))
        bottomAppBar.menu.clear()

        // 項目を編集する
        items.forEachIndexed { i, item ->
            item.toMenuItem(bottomAppBar.menu, tint).apply {
                setOnMenuItemClickListener {
                    addItem(i, UserBottomItem.SCROLL_TO_TOP)
                    true
                }
            }
        }

        // 項目を追加する
        if (maxButtonsNum - items.size > 0) {
            bottomAppBar.menu.add("追加").apply {
                setIcon(R.drawable.ic_baseline_add)
                MenuItemCompat.setIconTintList(this, tint)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                setOnMenuItemClickListener {
                    addItem(items.size, UserBottomItem.PREFERENCES)
                    true
                }
            }
        }
    }

    /** アイテムを追加 */
    private fun addItem(position: Int, item: UserBottomItem) {
        // TODO: skeleton
        val newItems =
            if (position >= items.size) items.plus(item)
            else items.mapIndexed { i, existed ->
                if (i == position) item
                else existed
            }

        itemsLiveData?.value = newItems

        // ビューを更新
        inflateButtons()
    }
}
