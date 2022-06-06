package com.suihan74.satena.scenes.preferences

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemGeneralButtonBinding
import com.suihan74.satena.databinding.ListviewItemGeneralSectionBinding
import com.suihan74.satena.databinding.ListviewItemPrefsButtonBinding
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import kotlinx.coroutines.flow.Flow

/**
 * 設定項目表示用アダプタ
 */
class PreferencesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<PreferencesAdapter.Item, PreferencesAdapter.ViewHolder>(
    DiffCallback()
) {
    private var onItemClickListener : Listener<Item>? = null

    fun overrideItemClickListener(listener: Listener<Item>?) {
        onItemClickListener = listener
    }

    // ------ //

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
        onItemClickListener?.let { listener ->
            holder.binding.root.setOnClickListener {
                listener.invoke(currentList[position])
            }
        }
    }

    // ------ //

    class ViewHolder(val binding : ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    // ------ //

    interface Item {
        val layoutId : Int
        val description : String
        fun bind(binding: ViewDataBinding)
        fun areItemsTheSame(old: Item, new: Item) : Boolean = old === new
        fun areContentsTheSame(old: Item, new: Item) : Boolean = old == new
    }

    /**
     * セクション名
     */
    open class Section(@StringRes val textId: Int) : Item {
        override val layoutId : Int = R.layout.listview_item_general_section

        override val description: String =
            SatenaApplication.instance.getString(textId)

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
        val text: LiveData<CharSequence>,
        val subText: LiveData<CharSequence>? = null,
        val textColor: LiveData<Int>? = null,
        val subTextColor: LiveData<Int>? = null,
        var onLongClick : (()->Unit)? = null,
        var onClick : (()->Unit)? = null
    ) : Item {
        override val layoutId : Int = R.layout.listview_item_general_button

        override val description: String = text.value?.toString().orEmpty()

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemGeneralButtonBinding> {
                it.text = text
                it.subText = subText
                it.mainTextColor = textColor
                it.subTextColor = subTextColor
                it.root.setOnClickListener {
                    onClick?.invoke()
                }
                it.root.setOnLongClickListener {
                    onLongClick?.invoke() != null
                }
            }
        }
        override fun areItemsTheSame(old: Item, new: Item): Boolean =
            old is Button && new is Button && old.text.value == new.text.value

        override fun areContentsTheSame(old: Item, new: Item): Boolean =
            old is Button && new is Button &&
                    old.text.value == new.text.value &&
                    old.subText?.value == new.subText?.value &&
                    old.textColor?.value == new.textColor?.value &&
                    old.subTextColor?.value == new.subTextColor?.value &&
                    old.onLongClick == new.onLongClick &&
                    old.onClick == new.onClick
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
open class PreferenceItem<T> : PreferencesAdapter.Item {
    constructor(
        liveData: LiveData<T>,
        @StringRes titleId: Int,
        textConverter: ((Context, Any) -> String)? = null,
        onLongClick: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        this.liveData = liveData
        this.titleId = titleId
        this.textConverter = textConverter ?: { context, value -> defaultTextConverter(context, value, titleId) }
        this.onLongClick = onLongClick
        this.onClick = onClick
    }

    constructor(
        flow: Flow<T>,
        @StringRes titleId: Int,
        textConverter: ((Context, Any) -> String)? = null,
        onLongClick: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        this.liveData = flow.asLiveData()
        this.titleId = titleId
        this.textConverter = textConverter ?: { context, value -> defaultTextConverter(context, value, titleId) }
        this.onLongClick = onLongClick
        this.onClick = onClick
    }

    // ------ //

    val liveData: LiveData<T>

    @StringRes
    val titleId: Int

    val textConverter: (Context, Any) -> String

    private val onLongClick: (() -> Unit)?

    private val onClick: (() -> Unit)?

    // ------ //

    override val layoutId: Int
        get() = R.layout.listview_item_prefs_button

    override val description: String
        get() = SatenaApplication.instance.getString(titleId)

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

    override fun areItemsTheSame(
        old: PreferencesAdapter.Item,
        new: PreferencesAdapter.Item
    ): Boolean =
        old is PreferenceItem<*> && new is PreferenceItem<*> && old.titleId == new.titleId

    override fun areContentsTheSame(
        old: PreferencesAdapter.Item,
        new: PreferencesAdapter.Item
    ): Boolean =
        old is PreferenceItem<*> && new is PreferenceItem<*> &&
                old.liveData.value == new.liveData.value &&
                old.titleId == new.titleId &&
                old.textConverter == new.textConverter &&
                old.onLongClick == new.onLongClick &&
                old.onClick == new.onClick

    companion object {
        fun <T> defaultTextConverter(context: Context, value: T?, titleId: Int): String =
            when (value) {
                is Number -> value.toString()
                is Boolean -> context.getString(if (value) R.string.on else R.string.off)
                is TextIdContainer -> context.getString(value.textId)
                null -> {
                    val text = context.getString(titleId)
                    FirebaseCrashlytics.getInstance().recordException(NullPointerException("value is null; title=`$text`"))
                    "null"
                }
                else -> value.toString()
            }
    }
}

/**
 * 設定用トグルボタン
 */
open class PreferenceToggleItem(
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int,
    textConverter: ((Context, Any) -> String)? = null,
    action: ((Boolean)->Unit)? = null
) : PreferenceItem<Boolean>(liveData, titleId, textConverter, null, {
    action?.invoke(liveData.value == true) ?: run {
        liveData.value = liveData.value != true
    }
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
    context: Context,
    @StringRes textId: Int,
    @StringRes subTextId: Int? = null,
    @ColorRes textColorId: Int? = null,
    @ColorRes subTextColorId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) : Boolean {
    val button = PreferencesAdapter.Button(
        text = MutableLiveData(context.getText(textId)),
        subText = subTextId?.let { MutableLiveData(context.getText(it)) },
        textColor = textColorId?.let { MutableLiveData(context.getColor(it)) },
        subTextColor = subTextColorId?.let { MutableLiveData(context.getColor(it)) },
        onLongClick = onLongClick,
        onClick = onClick
    )
    return add(button)
}

/**
 * ボタン(非設定項目)を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addButton(
    text: LiveData<CharSequence>,
    subText: LiveData<CharSequence>? = null,
    textColor: LiveData<Int>? = null,
    subTextColor: LiveData<Int>? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferencesAdapter.Button(
    text = text,
    subText = subText,
    textColor = textColor,
    subTextColor = subTextColor,
    onLongClick = onLongClick,
    onClick = onClick
))

/**
 * ボタン(非設定項目)を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addButton(
    context: Context,
    text: LiveData<CharSequence>,
    subText: LiveData<CharSequence>? = null,
    textColorId: Int? = null,
    subTextColorId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) : Boolean {
    val button = PreferencesAdapter.Button(
        text = text,
        subText = subText,
        textColor = textColorId?.let { MutableLiveData(context.getColor(it)) },
        subTextColor = subTextColorId?.let { MutableLiveData(context.getColor(it)) },
        onLongClick = onLongClick,
        onClick = onClick
    )
    return add(button)
}

// ------ //

/**
 * 設定項目を追加する
 */
fun <T> MutableList<PreferencesAdapter.Item>.addPrefItem(
    liveData: LiveData<T>,
    @StringRes titleId: Int,
    textConverter: ((Context, Any) -> String)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferenceItem(liveData, titleId, textConverter, onLongClick, onClick))

/**
 * 設定項目を追加する
 */
fun <T> MutableList<PreferencesAdapter.Item>.addPrefItem(
    flow: Flow<T>,
    @StringRes titleId: Int,
    textConverter: ((Context, Any) -> String)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) = add(PreferenceItem(flow, titleId, textConverter, onLongClick, onClick))

/**
 * 真偽値をトグルする設定項目を追加する
 */
fun MutableList<PreferencesAdapter.Item>.addPrefToggleItem(
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int,
    textConverter: ((Context, Any) -> String)? = null,
    action: ((Boolean)->Unit)? = null
) = add(PreferenceToggleItem(liveData, titleId, textConverter, action))

fun MutableList<PreferencesAdapter.Item>.addPrefToggleItem(
    liveData: MutableLiveData<Boolean>,
    @StringRes titleId: Int,
    action: ((Boolean)->Unit)?
) = add(PreferenceToggleItem(liveData, titleId, null, action))

// ------ //

object PreferencesAdapterBindingAdapters {
    /**
     * 値が与えられた場合にテキストカラーを上書きする
     */
    @JvmStatic
    @BindingAdapter("textColorOverlap")
    fun bindTextColorOverlap(textView: TextView, color: Int?) {
        textView.setTextColor(
            color ?: textView.context.getThemeColor(R.attr.textColor)
        )
    }

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

    @JvmStatic
    @BindingAdapter("currentPreference", "converter")
    fun bindPref(textView: TextView, liveData: LiveData<*>?, converter: ((Context, Any)->String)?) {
        val context = textView.context
        textView.text =
            liveData?.value?.let { value ->
                converter?.invoke(context, value) ?: when (value) {
                    is Number -> value.toString()
                    is Boolean -> context.getString(if (value) R.string.on else R.string.off)
                    is TextIdContainer -> context.getString(value.textId)
                    else -> value.toString()
                }
            }.orEmpty()
    }
}
