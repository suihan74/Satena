package com.suihan74.satena.scenes.entries

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.GravityCompat
import com.suihan74.satena.R
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast

fun Spinner.initialize(
    context: Context,
    items: List<String>,
    iconId: Int,
    hint: String = "",
    onItemSelected: ((Int?)->Unit)? = null
) {
    val innerItems = listOf("*").plus(items)

    gravity = GravityCompat.END
    background = context.getDrawable(iconId)
    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.colorPrimaryText))
    adapter = object : ArrayAdapter<String>(
        context,
        R.layout.spinner_drop_down_item,
        innerItems
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).apply {
                if (this is TextView) {
                    this.text = ""
                }
            }
        }

        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = super.getDropDownView(position, convertView, parent)
            if (position == 0) {
                (view as TextView).apply {
                    text = "指定なし"
                    setTextColor(context.getColor(R.color.colorPrimary))
                }
            }
            else {
                (view as TextView).apply {
                    setTextColor(context.getThemeColor(R.attr.textColor))
                }
            }
            return view
        }
    }

    setOnLongClickListener {
        context.showToast(hint)
        return@setOnLongClickListener true
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
