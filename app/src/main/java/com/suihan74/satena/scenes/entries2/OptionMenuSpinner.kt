package com.suihan74.satena.scenes.entries2

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.MenuItemCompat
import com.suihan74.satena.R
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.getThemeColor

fun Spinner.initialize(
    context: Context,
    menuItem: MenuItem?,
    items: List<String>,
    tooltipTextId: Int? = null,
    onItemSelected: Listener<Int?>? = null
) {
    val innerItems = listOf("*").plus(items)

    if (menuItem != null) {
        // 「▼」を消す
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

        foreground = menuItem.icon
        foregroundTintList = MenuItemCompat.getIconTintList(menuItem)
        foregroundGravity = Gravity.CENTER
    }

    if (tooltipTextId == null) {
        TooltipCompat.setTooltipText(this, null)
    }
    else {
        val tooltipText = context.getString(tooltipTextId)
        TooltipCompat.setTooltipText(this, tooltipText)
    }

    adapter = object : ArrayAdapter<String>(
        context,
        R.layout.spinner_drop_down_item,
        innerItems
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).apply {
                if (this is TextView) {
                    text = ""
                    setPadding(0, 0, 0, 0)
                }
            }
        }

        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = super.getDropDownView(position, convertView, parent)
            (view as TextView).apply {
                if (position == 0) {
                    setText(R.string.option_menu_spinner_no_selected)
                }
                setTextColor(
                    if (position == this@initialize.selectedItemPosition)
                        context.getColor(R.color.colorPrimary)
                    else
                        context.getThemeColor(R.attr.textColor)
                )
            }
            return view
        }
    }

    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            if (position == 0) {
                onItemSelected?.invoke(null)
            }
            else {
                onItemSelected?.invoke(position - 1)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            onItemSelected?.invoke(null)
        }
    }
}
