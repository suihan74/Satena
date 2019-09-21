package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.fragments.NoticesFragment
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.setHtml
import org.threeten.bp.format.DateTimeFormatter

open class NoticesAdapter
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = ArrayList<RecyclerState<Notice>>()

    fun setNotices(notices : List<Notice>) {
        val items = notices
            .map {
                val objects = it.objects.distinctBy { obj -> obj.user }
                Notice(
                    created = it.created,
                    modified = it.modified,
                    objects = objects,
                    user = it.user,
                    verb = it.verb,
                    link = it.link,
                    metadata = it.metadata
                )
            }
        states.clear()
        states.addAll(RecyclerState.makeStatesWithFooter(items))
        notifyDataSetChanged()
    }

    class ViewHolder(private val view : View) : RecyclerView.ViewHolder(view) {
        private val icon      = view.findViewById<ImageView>(R.id.notice_icon)!!
        private val message   = view.findViewById<TextView>(R.id.notice_message)!!
        private val timestamp = view.findViewById<TextView>(R.id.notice_timestamp)!!

        var notice : Notice? = null
            internal set(value) {
                field = value

                if (value != null) {
                    message.setHtml(NoticesFragment.createMessage(value, view.context))
                    timestamp.text = value.modified.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))

                    val url = HatenaClient.getUserIconUrl(value.objects.last().user)
                    Glide.with(view)
                        .load(url)
                        .into(icon)
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_notices, parent, false)
                val holder = ViewHolder(inflate)

                holder.itemView.setOnClickListener {
                    val position = holder.adapterPosition
                    val notice = states[position].body!!
                    onItemClicked(notice)
                }

                holder.itemView.setOnLongClickListener {
                    val position = holder.adapterPosition
                    val notice = states[position].body!!
                    onItemLongClicked(notice)
                }

                return holder
            }

            else -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                return FooterViewHolder(inflate)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY ->
                (holder as ViewHolder).notice = states[position].body!!

            else -> {}
        }
    }

    override fun getItemCount() = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    open fun onItemClicked(notice : Notice) {
    }

    open fun onItemLongClicked(notice : Notice) : Boolean {
        return true
    }
}
