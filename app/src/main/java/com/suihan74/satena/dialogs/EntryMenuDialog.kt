package com.suihan74.satena.dialogs

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.R

class EntryMenuDialog : AlertDialogFragment() {
    interface Listener {
        fun onItemSelected(item: String, dialog: EntryMenuDialog)
    }

    companion object {
        private const val KEY_BASE = "EntryMenuDialog."
        const val ENTRY = KEY_BASE + "ENTRY"
    }

    val entry: Entry by lazy { arguments!!.getSerializable(ENTRY) as Entry }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = parentFragment as? Listener ?: activity as? Listener

        val arguments = arguments!!
        val themeResId = arguments.getInt(THEME_RES_ID)
        val items = arguments.getStringArray(ITEMS) ?: emptyArray()

        val rootUrlRegex = Regex("""https?://(.+)/$""")
        val rootUrl = rootUrlRegex.find(entry.rootUrl)?.groupValues?.get(1)
            ?: Uri.parse(entry.rootUrl).host
            ?: ""

        val titleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_entry, null).apply {
            findViewById<TextView>(R.id.title).text = if (entry.title.isBlank()) entry.url else entry.title
            findViewById<TextView>(R.id.domain).text = rootUrl
            val favicon = findViewById<ImageView>(R.id.favicon).apply {
                visibility = View.VISIBLE
            }
            Glide.with(context)
                .load(Uri.parse(entry.faviconUrl))
                .into(favicon)
        }

        return AlertDialog.Builder(requireContext(), themeResId)
            .setCustomTitle(titleView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(items) { _, which ->
                listener?.onItemSelected(items[which], this)
                this.dismissAllowingStateLoss()
            }
            .create()
    }

    class Builder {
        constructor(entry: Entry, themeResId: Int) {
            arguments = Bundle().apply {
                putInt(THEME_RES_ID, themeResId)
                putSerializable(ENTRY, entry)
            }
        }

        constructor(url: String, themeResId: Int) {
            arguments = Bundle().apply {
                putInt(THEME_RES_ID, themeResId)
                putSerializable(ENTRY, Entry(
                    id = 0,
                    title = "",
                    description = "",
                    count = 0,
                    url = url,
                    rootUrl = Uri.parse(url).let { it.scheme!! + "://" + it.host!! },
                    faviconUrl = null,
                    imageUrl = ""))
            }
        }

        private val arguments: Bundle

        fun create() : EntryMenuDialog {
            return EntryMenuDialog().apply {
                arguments = this@Builder.arguments
            }
        }

        fun show(fragmentManager: FragmentManager, tag: String) =
            create().show(fragmentManager, tag)

        fun setItems(items: Collection<String>) = this.apply {
            arguments.putStringArray(ITEMS, items.toTypedArray())
        }
    }
}
