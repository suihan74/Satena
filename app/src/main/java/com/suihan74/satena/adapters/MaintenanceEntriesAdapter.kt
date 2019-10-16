package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.MaintenanceEntry
import com.suihan74.satena.R
import com.suihan74.utilities.*
import org.threeten.bp.format.DateTimeFormatter

open class MaintenanceEntriesAdapter(entries : List<MaintenanceEntry>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = RecyclerState.makeStatesWithFooter(entries)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_maintenance_entries, parent, false)
                val holder = ViewHolder(inflate)

                holder.itemView.setOnClickListener {
                    holder.switchVisibility()
                }

                holder.itemView.setOnLongClickListener {
                    return@setOnLongClickListener true
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
            RecyclerType.BODY -> (holder as ViewHolder).entry = states[position].body!!
            else -> {}
        }
    }

    override fun getItemCount() = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    private class ViewHolder(private val root : View) : RecyclerView.ViewHolder(root) {
        private val title      = root.findViewById<TextView>(R.id.title)!!
        private val timestamp  = root.findViewById<TextView>(R.id.timestamp)!!
        private val body       = root.findViewById<TextView>(R.id.body)!!

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

        fun switchVisibility() {
            body.visibility = (body.visibility != View.VISIBLE).toVisibility()
        }

        var entry : MaintenanceEntry? = null
            internal set(value) {
                field = value
                if (value == null) return

                val resolvedColor = ContextCompat.getColor(root.context, R.color.maintenanceResolved)
                title.setHtml(value.title.replace("【復旧済み】", "<font color=\"$resolvedColor\">【復旧済み】</font>"))

                timestamp.text =
                    if (value.timestamp == value.timestampUpdated) {
                        value.timestamp.format(dateTimeFormatter)
                    }
                    else {
                        buildString {
                            append(value.timestamp.format(dateTimeFormatter))
                            append("  (更新: ", value.timestampUpdated.format(dateTimeFormatter), ")")
                        }
                    }

                body.visibility = View.GONE
                body.text = value.body
            }
    }
}
