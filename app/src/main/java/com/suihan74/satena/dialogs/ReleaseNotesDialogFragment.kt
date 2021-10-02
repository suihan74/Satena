package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogReleaseNotes2Binding
import com.suihan74.satena.databinding.ListviewItemReleaseNotesBinding
import com.suihan74.satena.databinding.ListviewSeparatorReleaseNotesBinding
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.SectionViewHolder
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

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
            putString(ARG_CURRENT_VERSION_NAME, currentVersionName)
            putString(ARG_LAST_VERSION_NAME, lastVersionName)
        }

        /** 最後に起動したときのバージョン */
        private const val ARG_LAST_VERSION_NAME = "ARG_LAST_VERSION_NAME"

        /** 現在実行中のバージョン */
        private const val ARG_CURRENT_VERSION_NAME = "ARG_CURRENT_VERSION_NAME"
    }

    // ------ //

    val viewModel by lazyProvideViewModel {
        DialogViewModel(
            ReleaseNotesRepository(
                resources,
                currentVersionName = arguments?.getString(ARG_CURRENT_VERSION_NAME),
                lastVersionName = arguments?.getString(ARG_LAST_VERSION_NAME)
            )
        )
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentDialogReleaseNotes2Binding.inflate(localLayoutInflater(), null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }

        // 下端までスクロールで追加分取得
        binding.recyclerView.apply {
            val updater = RecyclerViewScrollingUpdater {
                this.isEnabled = false
                lifecycleScope.launchWhenResumed {
                    viewModel.loadReleaseNotes()
                    this@RecyclerViewScrollingUpdater.loadCompleted()
                }
            }
            updater.isEnabled = false
            addOnScrollListener(updater)
            adapter = ReleaseNotesAdapter(updater)
        }

        if (savedInstanceState != null) {
            viewModel.onRestore(resources)
        }

        return createBuilder()
            .setTitle(R.string.release_notes_dialog_title)
            .setNegativeButton(R.string.dialog_close, null)
            .setView(binding.root)
            .create()
    }

    override fun onDestroy() {
        runCatching { viewModel.close() }
        super.onDestroy()
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
            recyclerView.adapter.alsoAs<ReleaseNotesAdapter> { adapter ->
                adapter.submitList(items.orEmpty())
            }
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

    class ReleaseNotesRepository(
        resources: Resources,
        /** 現在実行中のバージョン */
        val currentVersionName: String?,
        /** 最後に起動したときのバージョン */
        val lastVersionName: String?
    ) : Closeable {
        /** 逐次読み込みのために`BufferedReader`を保持する */
        private var bufferedReader = resources.openRawResource(R.raw.release_notes).bufferedReader()

        /**
         * 次に読み込む行の内容
         *
         * 各履歴項目の終了を「次行が次の履歴のタイトル部分かどうか」で判定するため
         */
        private var nextLine : String? = runCatching { bufferedReader.readLine() }.getOrNull()

        private var linesCount : Int = 0

        private var lastLoadedVersion : String? = null

        private val _releaseNotes = MutableLiveData<List<ReleaseNote>>()
        /** 更新履歴リスト */
        val releaseNotes : LiveData<List<ReleaseNote>> = _releaseNotes

        // ------ //

        fun onRestore(resources: Resources) {
            bufferedReader = resources.openRawResource(R.raw.release_notes).bufferedReader()
            repeat(linesCount) {
                nextLine = runCatching { bufferedReader.readLine() }.getOrNull()
            }
        }

        override fun close() {
            runCatching { bufferedReader.close() }
        }

        // ------ //

        /**
         * バッファされた次の行内容を返し、release_notes.txtからさらに次の行を読み込む
         *
         * @return 終端or読み込み失敗時にnull
         */
        private fun readLine() : String? {
            val result = nextLine
            nextLine = runCatching { bufferedReader.readLine() }.getOrNull()
            if (nextLine != null) linesCount++
            return result
        }

        @OptIn(ExperimentalStdlibApi::class)
        suspend fun loadNextItems(num: Int) = coroutineScope {
            if (lastVersionName != null && lastVersionName == lastLoadedVersion) return@coroutineScope

            val newItems = buildList {
                val titleRegex = Regex("""^\s*\[\s*version\s*([0-9.]+)\s*]\s*$""")
                val separatorRegex = Regex("""^----*$""")

                repeat(num) {
                    var insertSeparator = false

                    val title = buildString {
                        while (true) {
                            val line = readLine() ?: return@buildList
                            val matchResult = titleRegex.find(line)
                            if (matchResult != null) {
                                val code = matchResult.groupValues[1]
                                // 差分表示時は前回起動時のバージョンまで表示したら処理終了する
                                lastLoadedVersion = code
                                if (lastVersionName == code) {
                                    return@buildList
                                }

                                append("[ version $code ]")
                                break
                            }
                        }
                    }

                    val body = buildString {
                        do {
                            val line = readLine() ?: break
                            if (separatorRegex.matches(line)) {
                                insertSeparator = true
                                break
                            }
                            else {
                                append(line)
                                if (line.isNotBlank()) {
                                    append("\n")
                                }
                            }
                        } while (nextLine?.matches(titleRegex) != true)
                    }

                    add(ReleaseNote(title, body))

                    if (insertSeparator) {
                        add(ReleaseNote("---", ""))
                    }
                }
            }

            withContext(Dispatchers.Default) {
                _releaseNotes.postValue(
                    if (newItems.isEmpty()) _releaseNotes.value.orEmpty()
                    else _releaseNotes.value.orEmpty().plus(newItems)
                )
            }
        }
    }

    // ------ //

    class DialogViewModel(private val repository: ReleaseNotesRepository) : ViewModel(), Closeable {
        /** 最後に起動したときのバージョン */
        val lastVersionName = repository.lastVersionName

        /** 現在実行中のバージョン */
        val currentVersionName = repository.currentVersionName

        /** 差分のみを表示する */
        val displayOnlyDiffs = lastVersionName != null && currentVersionName != null

        /** 更新履歴 */
        val releaseNotes = repository.releaseNotes

        // ------ //

        init {
            viewModelScope.launch {
                loadReleaseNotes()
            }
        }

        fun onRestore(resources: Resources) {
            repository.onRestore(resources)
        }

        override fun close() {
            repository.close()
        }

        // ------ //

        /**
         * 履歴をロードする
         *
         * @throws TaskFailureException
         */
        @OptIn(ExperimentalStdlibApi::class)
        suspend fun loadReleaseNotes() = withContext(Dispatchers.Default) {
            repository.loadNextItems(num = 10)
        }
    }

    // ------ //

    /**
     * 更新履歴リストを表示するためのアダプタ
     */
    class ReleaseNotesAdapter(
        private val scrollingUpdater : RecyclerViewScrollingUpdater
    ) : ListAdapter<ReleaseNote, RecyclerView.ViewHolder>(DiffCallback()) {

        override fun submitList(list: List<ReleaseNote>?) {
            super.submitList(list) {
                scrollingUpdater.isEnabled = true
            }
        }

        override fun getItemViewType(position: Int) : Int =
            when (currentList[position].title) {
                "---" -> ViewHolderType.SEPARATOR.ordinal
                else -> ViewHolderType.CONTENT.ordinal
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                ViewHolderType.CONTENT.ordinal ->
                    ReleaseNoteViewHolder(
                        ListviewItemReleaseNotesBinding.inflate(layoutInflater, parent, false)
                    )

                ViewHolderType.SEPARATOR.ordinal ->
                    SectionViewHolder(
                        ListviewSeparatorReleaseNotesBinding.inflate(layoutInflater, parent, false)
                    )

                else -> throw NotImplementedError()
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.alsoAs<ReleaseNoteViewHolder> {
                it.setModel(currentList[position])
            }
        }

        // ------ //

        class ReleaseNoteViewHolder(
            private val binding : ListviewItemReleaseNotesBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun setModel(model: ReleaseNote) {
                binding.titleTextView.text = model.title
                binding.bodyTextView.text = model.body
                binding.root.setOnClickListener {}
            }
        }

        // ------ //

        enum class ViewHolderType {
            CONTENT,
            SEPARATOR
        }

        // ------ //

        class DiffCallback : DiffUtil.ItemCallback<ReleaseNote>() {
            override fun areItemsTheSame(oldItem: ReleaseNote, newItem: ReleaseNote): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: ReleaseNote, newItem: ReleaseNote): Boolean {
                return oldItem.body == newItem.body
            }
        }
    }
}
