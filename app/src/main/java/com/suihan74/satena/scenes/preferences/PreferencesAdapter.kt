package com.suihan74.satena.scenes.preferences

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
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
import com.suihan74.satena.databinding.ListviewItemPrefsEditTextBinding
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.utilities.extensions.alsoAs

/**
 * 設定項目表示用アダプタ
 */
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
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentList[position].bind(holder.binding)
        holder.binding.lifecycleOwner = lifecycleOwner
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

    /**
     * セクション名
     */
    open class Section(@StringRes val textId: Int) : Item {
        override val layoutId : Int = R.layout.listview_item_general_section
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralSectionBinding> {
                it.textId = textId
            }
        }

        override fun areItemsTheSame(old: Item, new: Item): Boolean =
            old is Section && new is Section && old.textId == new.textId

        override fun areContentsTheSame(old: Item, new: Item): Boolean =
            old is Section && new is Section && old == new
    }

    /**
     * 汎用的なボタン
     */
    open class Button(
        val fragment: Fragment,
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
        override fun areItemsTheSame(old: Item, new: Item): Boolean =
            old is Button && new is Button && old.textId == new.textId

        override fun areContentsTheSame(old: Item, new: Item): Boolean =
            old is Button && new is Button &&
                    old.fragment == new.fragment &&
                    old.textId == new.textId
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
    val fragment: Fragment,
    val liveData: LiveData<T>,
    @StringRes val titleId: Int,
    @StringRes val suffixId: Int? = null,
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

    override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) : Boolean =
        old is PreferenceItem<*> && new is PreferenceItem<*> && old.titleId == new.titleId

    override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) : Boolean =
        old is PreferenceItem<*> && new is PreferenceItem<*> &&
                old.fragment == new.fragment &&
                old.titleId == new.titleId &&
                old.suffixId == new.suffixId &&
                old.liveData.value == new.liveData.value
}

/**
 * 設定用トグルボタン
 */
open class PreferenceToggleItem(
    fragment: Fragment,
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int,
    @StringRes suffixId: Int? = null
) : PreferenceItem<Boolean>(fragment, liveData, titleId, suffixId, null, {
    liveData.value = liveData.value != true
})

/**
 * テキスト編集用ビュー
 */
open class PreferenceEditTextItem(
    val liveData: MutableLiveData<String>,
    @StringRes val titleId: Int,
    @StringRes val hintId: Int,
) : PreferencesAdapter.Item {
    override val layoutId: Int
        get() = R.layout.listview_item_prefs_edit_text

    override fun bind(binding: ViewDataBinding) {
        binding.alsoAs<ListviewItemPrefsEditTextBinding> { b ->
            b.item = this

            b.editText.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        b.editText.hideSoftInputMethod()
                        true
                    }

                    else -> false
                }
            }
        }
    }
}

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
    fragment: Fragment,
    @StringRes textId: Int,
    @StringRes subTextId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferencesAdapter.Button(fragment, textId, subTextId, onLongClick, onClick))

/**
 * 設定項目を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addPrefItem(
    fragment: Fragment,
    liveData: LiveData<*>,
    @StringRes titleId: Int,
    @StringRes suffixId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferenceItem(fragment, liveData, titleId, suffixId, onLongClick, onClick))

/**
 * 真偽値をトグルする設定項目を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addPrefToggleItem(
    fragment: Fragment,
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int,
    @StringRes suffixId: Int? = null,
    ) = add(PreferenceToggleItem(fragment, liveData, titleId, suffixId))

// ------ //

object PreferencesAdapterBindingAdapters {
    /**
     * 設定項目をビューに反映する
     */
    @JvmStatic
    @BindingAdapter("items")
    fun bindItems(recyclerView: RecyclerView, items: List<PreferencesAdapter.Item>?) {
        recyclerView.adapter.alsoAs<PreferencesAdapter> { adapter ->
            adapter.submitList(items)
        }
    }

    /**
     * 現在の設定値を表示する
     */
    @JvmStatic
    @BindingAdapter("currentPreference", "suffixId")
    fun bindPref(textView: TextView, liveData: LiveData<*>?, suffixId: Int?) {
        val context = textView.context
        val value = when (val value = liveData?.value) {
            is Number -> value.toString()

            is Boolean ->
                context.getString(
                    if (value) R.string.on
                    else R.string.off
                )

            is TextIdContainer -> context.getString(value.textId)

            else -> ""
        }
        val suffix = suffixId?.let { context.getText(it) } ?: ""
        textView.text = String.format("%s%s", value, suffix)
    }
}
