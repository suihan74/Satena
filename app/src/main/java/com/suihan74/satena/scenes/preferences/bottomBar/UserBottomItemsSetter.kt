package com.suihan74.satena.scenes.preferences.bottomBar

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuItemCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ViewUserBottomItemsSetterBinding
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.views.bindMenuItemsGravity

class UserBottomItemsSetter : CoordinatorLayout {
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("items")
        fun bindItems(
            instance: UserBottomItemsSetter,
            liveData: MutableLiveData<List<UserBottomItem>>?
        ) {
            instance.itemsLiveData = liveData
            instance.inflateButtons()
        }

        @JvmStatic
        @BindingAdapter("menuGravity")
        fun bindMenuGravity(
            instance: UserBottomItemsSetter,
            gravity: Int
        ) {
            instance.binding.bottomAppBar.bindMenuItemsGravity(gravity)
            instance.inflateButtons()
        }
    }

    // ------ //

    companion object {
        /** 表示できるボタン数の最大値を取得する */
        fun getButtonsLimit(context: Context) : Int {
            val displayMetrics = context.resources.displayMetrics
            val density = displayMetrics.density

            val screenWidthPx = displayMetrics.widthPixels
            val rightMargin = (96 * density).toInt()
            val buttonWidthPx = (48 * density).toInt()

            // カテゴリによって追加されるボタンの最大値(暫定)
            val numReserved = 2

            return (screenWidthPx - rightMargin) / buttonWidthPx - numReserved
        }
    }

    // ------ //

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleInt: Int
    ) : super(context, attrs, defStyleInt) {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.view_user_bottom_items_setter,
            this,
            true
        )
    }

    /** バインド */
    private val binding: ViewUserBottomItemsSetterBinding

    /** 表示する項目 */
    private var itemsLiveData : MutableLiveData<List<UserBottomItem>>? = null
    private val items: List<UserBottomItem>
        get() = itemsLiveData?.value ?: emptyList()

    /** 表示できるボタンの最大数 */
    private val maxButtonsNum : Int by lazy {
        getButtonsLimit(context)
    }

    data class OnMenuItemClickArguments (
        val items: List<UserBottomItem>,
        val target: UserBottomItem?
    )

    /** ボタンを押したときのイベントリスナ */
    private var onMenuItemClickListener: Listener<OnMenuItemClickArguments>? = null

    /** ボタンを押したときのイベントリスナをセットする */
    fun setOnMenuItemClickListener(listener: Listener<OnMenuItemClickArguments>) {
        onMenuItemClickListener = listener
    }

    /** 設定用のBottomAppBarにボタンを表示する */
    private fun inflateButtons() {
        val items = items.take(maxButtonsNum)

        val bottomAppBar = binding.bottomAppBar
        val tint = ColorStateList.valueOf(context.getThemeColor(R.attr.textColor))
        bottomAppBar.menu.clear()

        // 項目を編集する
        items.forEachIndexed { i, item ->
            item.toMenuItem(bottomAppBar.menu, tint).apply {
                setOnMenuItemClickListener {
                    onMenuItemClickListener?.invoke(OnMenuItemClickArguments(items, item))
                    onMenuItemClickListener != null
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
                    onMenuItemClickListener?.invoke(OnMenuItemClickArguments(items, null))
                    onMenuItemClickListener != null
                }
            }
        }
    }

    /** アイテムを追加 */
    private fun addItem(position: Int, item: UserBottomItem) {
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

