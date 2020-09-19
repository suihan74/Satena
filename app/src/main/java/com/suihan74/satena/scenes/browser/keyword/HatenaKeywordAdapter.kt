package com.suihan74.satena.scenes.browser.keyword

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.utilities.extensions.makeSpannedFromHtml
import kotlinx.android.synthetic.main.listview_item_hatena_keyword.view.*

/** キーワードの解説項目を表示するアダプタ */
class HatenaKeywordAdapter(
    private val items: List<Keyword>
) : RecyclerView.Adapter<HatenaKeywordAdapter.ViewHolder>() {
    // 一度表示されたらあとで更新されたりはしないので簡単に作る

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.listview_item_hatena_keyword, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.setData(data)
    }

    // ------ //

    class ViewHolder(
        private val view: View
    ): RecyclerView.ViewHolder(view) {
        fun setData(data: Keyword) {
            view.title.text = data.category
            view.body.text = makeSpannedFromHtml(data.bodyHtml)
        }
    }
}
