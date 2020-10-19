package com.suihan74.satena.scenes.preferences.browser

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBlockingUrlSettingBinding
import com.suihan74.satena.scenes.browser.BlockUrlSetting
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.alsoAs

class BlockUrlSettingsAdapter(
    lifecycleOwner: LifecycleOwner
) :
    GeneralAdapter<BlockUrlSetting, ListviewItemBlockingUrlSettingBinding>(
        lifecycleOwner,
        R.layout.listview_item_blocking_url_setting,
        DiffCallback()
    )
{
    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("blockUrlSettings")
        fun setBlockUrlSettings(view: RecyclerView, items: List<BlockUrlSetting>?) {
            if (items == null) return
            view.adapter.alsoAs<BlockUrlSettingsAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }

    // ------ //

    override fun bind(
        model: BlockUrlSetting?,
        binding: ListviewItemBlockingUrlSettingBinding
    ) {
        binding.model = model
    }

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<BlockUrlSetting>() {
        override fun areModelsTheSame(
            oldItem: BlockUrlSetting?,
            newItem: BlockUrlSetting?
        ): Boolean {
            return oldItem?.isRegex == newItem?.isRegex
                    && oldItem?.pattern == newItem?.pattern
        }

        override fun areModelContentsTheSame(
            oldItem: BlockUrlSetting?,
            newItem: BlockUrlSetting?
        ): Boolean {
            return oldItem?.isRegex == newItem?.isRegex
                    && oldItem?.pattern == newItem?.pattern
        }
    }
}
