package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogReleaseNotes2Binding
import com.suihan74.satena.databinding.ListviewItemReleaseNotesBinding
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReleaseNotesDialogFragment : DialogFragment() {
    companion object {
        /**
         * すべての更新履歴を表示する
         */
        fun createInstance() = ReleaseNotesDialogFragment()

        /**
         * 前回起動時からの差分のみを表示する
         *
         * 更新後初回起動時に表示するときに使用
         */
        fun createInstance(
            lastVersionName: String,
            currentVersionName: String
        ) = ReleaseNotesDialogFragment().withArguments {
            putString(ARG_LAST_VERSION_NAME, lastVersionName)
            putString(ARG_CURRENT_VERSION_NAME, currentVersionName)
        }

        /** 最後に起動したときのバージョン */
        private const val ARG_LAST_VERSION_NAME = "ARG_LAST_VERSION_NAME"

        /** 現在実行中のバージョン */
        private const val ARG_CURRENT_VERSION_NAME = "ARG_CURRENT_VERSION_NAME"
    }

    // ------ //

    val viewModel by lazy {
        provideViewModel(this) {
            val lastVersionName = arguments?.getString(ARG_LAST_VERSION_NAME)
            val currentVersionName = arguments?.getString(ARG_CURRENT_VERSION_NAME)

            DialogViewModel(
                lastVersionName,
                currentVersionName
            )
        }
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<FragmentDialogReleaseNotes2Binding>(
            inflater,
            R.layout.fragment_dialog_release_notes2,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner ?: requireActivity()
        }

        // 履歴の読み込み
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                viewModel.loadReleaseNotes(resources)
            }
            catch (e: TaskFailureException) {
                context.showToast(R.string.msg_read_release_notes_failed)
            }
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(R.string.release_notes_dialog_title)
            .setNegativeButton(R.string.dialog_close, null)
            .setView(binding.root)
            .create()
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("lastVersionName", "currentVersionName")
        fun setUpdateVersionMessage(
            textView: TextView,
            lastVersionName: String?,
            currentVersionName: String?
        ) {
            textView.text =
                if (lastVersionName == null || currentVersionName == null) ""
                else textView.context?.getString(
                    R.string.release_notes_dialog_update_message,
                    lastVersionName,
                    currentVersionName
                ).orEmpty()
        }

        @JvmStatic
        @BindingAdapter("releaseNotes")
        fun setReleaseNotes(recyclerView: RecyclerView, items: List<ReleaseNote>?) {
            if (items.isNullOrEmpty()) return
            recyclerView.adapter = ReleaseNotesAdapter(items)
        }
    }

    // ------ //

    /**
     * 更新履歴の各項目
     */
    data class ReleaseNote(
        val title : String,
        val body : String
    )

    // ------ //

    class DialogViewModel(
        /** 最後に起動したときのバージョン */
        val lastVersionName : String?,

        /** 現在実行中のバージョン */
        val currentVersionName : String?
    ) : ViewModel() {

        /** 差分のみを表示する */
        val displayOnlyDiffs = lastVersionName != null && currentVersionName != null

        /** 更新履歴 */
        val releaseNotes = MutableLiveData<List<ReleaseNote>>()

        // ------ //

        /**
         * 履歴をロードする
         *
         * @throws TaskFailureException
         */
        suspend fun loadReleaseNotes(resources: Resources) = withContext(Dispatchers.Default) {
            try {
                resources.openRawResource(R.raw.release_notes).bufferedReader().use { reader ->
                    // 最後の起動時のバージョンが渡されている場合、そこから最新までの差分だけを表示する
                    val text = when (lastVersionName) {
                        null -> reader.readText()
                        else -> buildString {
                            reader.useLines { it.forEach { line ->
                                if (line.contains("[ version $lastVersionName ]")) {
                                    return@useLines
                                }
                                else {
                                    append(line, "\n")
                                }
                            } }
                        }
                    }

                    val historyRegex = Regex("""(\[\s*version\s*\S+\s*])(\r?\n)+([^\[]+)""")
                    val tailLineBreakRegex = Regex("""(\r?\n)+$""")

                    val items = historyRegex.findAll(text).mapNotNull { match ->
                        val result = runCatching {
                            ReleaseNote(
                                title = match.groupValues[1],
                                body = match.groupValues[3].replace(tailLineBreakRegex, "")
                            )
                        }
                        result.getOrNull()
                    }.toList()

                    releaseNotes.postValue(items)
                }
            }
            catch (e: Throwable) {
                throw TaskFailureException(cause = e)
            }
        }
    }

    // ------ //

    /**
     * 更新履歴リストを表示するためのアダプタ
     *
     * 一度初期化されたら後から更新されたりしないので、`RecyclerView.Adapter`で簡単に作っている
     */
    class ReleaseNotesAdapter(
        val items: List<ReleaseNote>
    ) : RecyclerView.Adapter<ReleaseNotesAdapter.ViewHolder>() {

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ListviewItemReleaseNotesBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setModel(items[position])
        }

        class ViewHolder(private val binding : ListviewItemReleaseNotesBinding) : RecyclerView.ViewHolder(binding.root) {
            fun setModel(model: ReleaseNote) {
                binding.titleTextView.text = model.title
                binding.bodyTextView.text = model.body
                binding.root.setOnClickListener {}
            }
        }
    }
}
