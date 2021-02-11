package com.suihan74.satena.scenes.preferences

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemGeneralButtonBinding
import com.suihan74.satena.databinding.ListviewItemGeneralSectionBinding
import com.suihan74.satena.databinding.ListviewItemPrefsButtonBinding
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.utilities.extensions.alsoAs

class PreferencesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<PreferencesAdapter.Item, PreferencesAdapter.ViewHolder>(
    DiffCallback()
) {
    override fun getItemViewType(position: Int): Int {
        return currentList[position].layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, layoutId: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(parent.context),
            layoutId,
            parent,
            false
        ).also {
            it.lifecycleOwner = lifecycleOwner
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentList[position].bind(holder.binding)
    }

    // ------ //

    class ViewHolder(val binding : ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    // ------ //

    interface Item {
        val layoutId : Int
        fun bind(binding: ViewDataBinding)
        fun areItemsTheSame(old: Item, new: Item) : Boolean = old == new
        fun areContentsTheSame(old: Item, new: Item) : Boolean = old == new
    }

    open class Section(@StringRes val textId: Int) : Item {
        override val layoutId : Int = R.layout.listview_item_general_section
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralSectionBinding> {
                it.textId = textId
            }
        }
    }

    open class Button(
        @StringRes val textId: Int,
        @StringRes val subTextId: Int? = null,
        var onLongClick : (()->Unit)? = null,
        var onClick : (()->Unit)? = null
    ) : Item {
        override val layoutId : Int = R.layout.listview_item_general_button
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralButtonBinding> {
                it.textId = textId
                it.subTextId = subTextId
                it.root.setOnClickListener {
                    onClick?.invoke()
                }
                it.root.setOnLongClickListener {
                    onLongClick?.invoke() != null
                }
            }
        }
    }

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.areContentsTheSame(oldItem, newItem)
        }
    }
}

// ------ //

/**
 * 設定アイテム
 */
open class PreferenceItem<T>(
    val liveData: LiveData<T>,
    @StringRes private val titleId: Int,
    private val onLongClick: (()->Unit)? = null,
    private val onClick: (()->Unit)? = null
) : PreferencesAdapter.Item {
    override val layoutId: Int
        get() = R.layout.listview_item_prefs_button

    override fun bind(binding: ViewDataBinding) {
        binding.alsoAs<ListviewItemPrefsButtonBinding> {
            it.titleId = titleId
            it.item = this
        }
        binding.root.setOnClickListener {
            onClick?.invoke()
        }
        binding.root.setOnLongClickListener {
            onLongClick?.invoke() != null
        }
    }
}

/**
 * トグルボタン
 */
class PreferenceToggleItem(
    liveData: MutableLiveData<Boolean>,
    @StringRes private val titleId: Int,
) : PreferenceItem<Boolean>(liveData, titleId, null, {
    liveData.value = liveData.value != true
})

// ------ //

/**
 * セクションを追加する
 */
fun MutableList<PreferencesAdapter.Item>.addSection(
    @StringRes textId: Int,
) = add(PreferencesAdapter.Section(textId))

/**
 * ボタン(非設定項目)を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addButton(
    @StringRes textId: Int,
    @StringRes subTextId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferencesAdapter.Button(textId, subTextId, onLongClick, onClick))

/**
 * 設定項目を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addPrefItem(
    liveData: LiveData<*>,
    @StringRes titleId: Int,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferenceItem(liveData, titleId, onLongClick, onClick))

/**
 * 真偽値をトグルする設定項目を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addPrefToggleItem(
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int
) = add(PreferenceToggleItem(liveData, titleId))

// ------ //

/**
 * 現在の設定値を表示する
 */
object PrefsTextViewBindingAdapters {
    @JvmStatic
    @BindingAdapter("currentPreference")
    fun bindPref(textView: TextView, liveData: LiveData<*>?) {
        val context = textView.context
        textView.text = when (val value = liveData?.value) {
            is Number -> value.toString()

            is Boolean ->
                context.getString(
                    if (value) R.string.on
                    else R.string.off
                )

            is TextIdContainer -> context.getString(value.textId)

            else -> ""
        }
    }
}
