package com.suihan74.satena.scenes.browser.keyword

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.databinding.ListviewItemHatenaKeywordBinding
import com.suihan74.utilities.extensions.makeSpannedFromHtml

/** キーワードの解説項目を表示するアダプタ */
class HatenaKeywordAdapter(
    private val items: List<Keyword>
) : RecyclerView.Adapter<HatenaKeywordAdapter.ViewHolder>() {
    // 一度表示されたらあとで更新されたりはしないので簡単に作る

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListviewItemHatenaKeywordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.setData(data)
    }

    // ------ //

    class ViewHolder(
        private val binding: ListviewItemHatenaKeywordBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun setData(data: Keyword) {
            binding.title.text = data.category
            binding.body.text = makeSpannedFromHtml(data.bodyHtml)
        }
    }
}
