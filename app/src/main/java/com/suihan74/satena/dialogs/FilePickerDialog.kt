package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.utilities.DividerItemDecorator
import java.io.File

/*
ファイル・ディレクトリリストを取得するには以下のパーミッション設定が必要

1.パーミッション指定
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

2.ランタイムパーミッションチェック
-> com.suihan74.utilities.RuntimePermission
*/

class FilePickerDialog : DialogFragment() {
    companion object {
        fun createInstance(title: String, message: String?, action: (FragmentManager, File)->Unit): FilePickerDialog = FilePickerDialog().apply {
            this.title = title
            this.message = message
            this.action = action
        }

        fun createInstance(title: String, action: (FragmentManager, File)->Unit) = FilePickerDialog().apply {
            this.title = title
            this.message = null
            this.action = action
        }

        private var savedTitle : String? = null
        private var savedMessage : String? = null
        private var savedAction : ((FragmentManager, File)->Unit)? = null
        private var savedDirectoryOnly : Boolean? = null
        private var savedCurrentDirectory : File? = null
    }

    private var title : String? = null
    private var message: String? = null
    private lateinit var action: (FragmentManager, File)->Unit
    var directoryOnly: Boolean = true

    private lateinit var mItemsAdapter: ItemsAdapter

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedTitle = title
        savedMessage = message
        savedAction = action
        savedDirectoryOnly = directoryOnly
        savedCurrentDirectory = mItemsAdapter.currentDirectory
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity as Context

        savedTitle?.let {
            title = it
            savedTitle = null
        }

        savedMessage?.let {
            message = it
            savedMessage = null
        }

        savedAction?.let {
            action = it
            savedAction = null
        }

        savedDirectoryOnly?.let {
            directoryOnly = it
            savedDirectoryOnly = null
        }

        val externalStorage = savedCurrentDirectory?.let { it } ?: Environment.getExternalStorageDirectory()
        savedCurrentDirectory = null

        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.fragment_dialog_filepicker, null)

        val itemsAdapter = ItemsAdapter(externalStorage, directoryOnly, root.findViewById(R.id.current_path))
        mItemsAdapter = itemsAdapter

        root.findViewById<RecyclerView>(R.id.file_list).apply {
            val dividerItemDecoration = DividerItemDecorator(
                ContextCompat.getDrawable(context, R.drawable.recycler_view_item_divider)!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = itemsAdapter
        }

        val builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setTitle(title)
            .setPositiveButton("開く") { _, _ ->
                action(fragmentManager!!, itemsAdapter.currentFile)
            }
            .setNegativeButton("CANCEL", null)
            .setView(root)

        if (message != null) {
            builder.setMessage(message)
        }

        return builder.create()
    }

    private class ItemsAdapter(
        private var mCurrentFile: File,
        private val directoryOnly: Boolean,
        private val currentPathView: TextView
    ) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        val fullPath: String
            get() = (selectedFile ?: mCurrentFile).absolutePath

        val currentDirectory: File
            get() = mCurrentFile

        val currentFile: File
            get() = selectedFile ?: mCurrentFile

        var selectedFile: File? = null
            private set

        private var files : Array<out File>

        init {
            files = makeFiles(mCurrentFile)
            currentPathView.text = mCurrentFile.absolutePath
        }

        private fun makeFiles(current: File) =
            if (current == Environment.getExternalStorageDirectory()) {
                emptyList()
            }
            else {
                listOf(current.parentFile)
            }.plus(
                if (directoryOnly) {
                    current.listFiles()
                        .filter { it.isDirectory }
                        .sortedBy { it.name }
                }
                else {
                    val list = current.listFiles()
                    list.filter { it.isDirectory }
                        .sortedBy { it.name }
                        .plus(
                            list.filterNot { it.isDirectory }
                                .sortedBy { it.name }
                        )
                })
                .toTypedArray()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_filepicker, parent, false)
            val holder = ViewHolder(inflate as TextView)

            holder.itemView.setOnClickListener {
                selectedFile = null
                val child = files[holder.adapterPosition]
                if (child.isDirectory) {
                    move(child)
                }
                else {
                    selectedFile = child
                    currentPathView.text = child.absolutePath
                }
            }

            return holder
        }

        override fun getItemCount(): Int = files.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            if (file.isDirectory) {
                holder.text = if (mCurrentFile.parentFile == file) {
                    "../"
                }
                else {
                    "${files[position].name}/"
                }
            }
            else {
                holder.text = files[position].name
            }
            holder.isDirectory = file.isDirectory
        }

        /** カレントディレクトリを移動（親か子かの相対移動のみ） */
        private fun move(child: File) {
            mCurrentFile = child
            files = makeFiles(mCurrentFile)
            currentPathView.text = mCurrentFile.absolutePath
            notifyDataSetChanged()
        }

        private class ViewHolder(private val root: TextView) : RecyclerView.ViewHolder(root) {
            var text: String = ""
                set(value) {
                    field = value
                    root.text = value
                }

            var isDirectory: Boolean = false
                set(value) {
                    field = value
                    root.setTextColor(if (value) {
                        ContextCompat.getColor(root.context, R.color.textColor)
                    }
                    else {
                        ContextCompat.getColor(root.context, R.color.colorPrimary)
                    })
                }
        }
    }
}
