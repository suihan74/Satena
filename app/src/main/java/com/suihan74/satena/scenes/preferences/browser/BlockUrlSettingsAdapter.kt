package com.suihan74.satena.scenes.preferences.browser

import androidx.lifecycle.LifecycleOwner
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemBlockingUrlSettingBinding
import com.suihan74.satena.scenes.browser.BlockUrlSetting
import com.suihan74.utilities.GeneralAdapter

class BlockUrlSettingsAdapter(
    lifecycleOwner: LifecycleOwner
) :
    GeneralAdapter<BlockUrlSetting, ListviewItemBlockingUrlSettingBinding>(
        lifecycleOwner,
        R.layout.listview_item_blocking_url_setting,
        DiffCallback()
    )
{
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
