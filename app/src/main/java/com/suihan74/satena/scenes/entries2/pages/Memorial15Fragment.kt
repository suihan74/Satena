package com.suihan74.satena.scenes.entries2.pages

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments


class Memorial15Fragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance() = Memorial15Fragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Memorial15th)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = Memorial15ViewModel.Factory()
        return ViewModelProvider(owner, factory)[viewModelKey, Memorial15ViewModel::class.java]
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        // 項目数が多いので、タブ部分をスクロールできるようにする
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        if (activity.viewModel.signedIn.value == true) {
            // メニューバーの設定
            if (bottomAppBar == null) {
                setHasOptionsMenu(true)
            }
            else {
                setHasOptionsMenu(false)
                activity.inflateExtraBottomMenu(R.menu.memorial_15th_bottom)
                initializeMenu(bottomAppBar.menu, bottomAppBar)
            }
        }

        return result
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.memorial_15th, menu)
        initializeMenu(menu)
    }

    private fun initializeMenu(menu: Menu, bottomAppBar: BottomAppBar? = null) {
        val viewModel = viewModel as Memorial15ViewModel
        val menuItem = menu.findItem(R.id.mode_spinner) ?: return
        val spinner = menuItem.actionView as? Spinner ?: return
        val items: Array<Int> = arrayOf(
            R.string.entries_tab_15th_entire,
            R.string.entries_tab_15th_user
        )

        val foregroundTint = MenuItemCompat.getIconTintList(menuItem)

        if (bottomAppBar == null) {
            // 「▼」の色をmenuリソースで指定する
            spinner.backgroundTintList = foregroundTint
        }
        else {
            // ボトムバーの場合、「▼」の代わりにアイコンを表示する
            spinner.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

            spinner.foreground = spinnerForeground(viewModel.isUserMode.value)
            spinner.foregroundTintList = foregroundTint
            spinner.foregroundGravity = Gravity.CENTER
        }

        // Tooltipテキストをmenuリソースで指定する
        TooltipCompat.setTooltipText(spinner, menuItem.title)

        spinner.adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_drop_down_item,
            items.map { getString(it) }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).alsoAs<TextView> {
                    if (bottomAppBar == null) {
                        it.setText(items[position])
                    }
                    else {
                        // ボトムバーの場合、選択中の項目を示すテキストは表示しない
                        it.text = ""
                        it.setPadding(0, 0, 0, 0)
                    }
                    it.setTextColor(foregroundTint)
                }!!

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View = super.getDropDownView(position, convertView, parent).alsoAs<TextView> {
                it.setTextColor(
                    if (position == spinner.selectedItemPosition)
                        context.getColor(R.color.colorPrimary)
                    else
                        context.getThemeColor(R.attr.textColor)
                )
            }!!
        }

        // 再開時にidx=0が再選択されてしまうので、明示的に防ぐ
        if (viewModel.isUserMode.value == true) {
            spinner.setSelection(items.indexOf(R.string.entries_tab_15th_user))
        }
        else {
            spinner.setSelection(0)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.isUserMode.value = items[position] == R.string.entries_tab_15th_user
                if (bottomAppBar != null) {
                    spinner.foreground = spinnerForeground(viewModel.isUserMode.value)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** モード選択用のアイコン */
    private fun spinnerForeground(isUserMode: Boolean?) = ContextCompat.getDrawable(
        requireContext(),
        if (isUserMode == true) R.drawable.ic_user_tag
        else R.drawable.ic_world
    )
}
