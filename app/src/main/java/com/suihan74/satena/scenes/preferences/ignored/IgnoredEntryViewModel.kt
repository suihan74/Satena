package com.suihan74.satena.scenes.preferences.ignored

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.utilities.Listener
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnSuccess
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IgnoredEntryViewModel(
    val repository: IgnoredEntriesRepository
) : ViewModel() {

    /** 全非表示設定 */
    private val allEntries = repository.ignoredEntries

    /** 非表示URL設定リスト */
    private val urlEntries = MutableLiveData<List<IgnoredEntry>>()

    /** NGワード設定リスト */
    private val wordEntries = MutableLiveData<List<IgnoredEntry>>()

    /** 画面に表示されるリスト */
    val displayedList = MutableLiveData<List<IgnoredEntry>>()

    /** リスト表示対象がURLorTEXTどちらか */
    var mode = IgnoredEntryType.URL
        private set

    /** 表示対象リスト更新時に表示用リストに内容反映するオブザーバ */
    private val listObserver = Observer<List<*>> {
        updateDisplayedList()
    }

    // ------ //

    init {
        viewModelScope.launch {
            repository.loadAllIgnoredEntries(forceUpdate = true)
        }
    }

    fun onCreate(lifecycleOwner: LifecycleOwner) {
        allEntries.observe(lifecycleOwner, Observer { allEntries ->
            viewModelScope.launch {
                updateLists(allEntries)
            }
        })
    }

    private suspend fun updateLists(allEntries: List<IgnoredEntry>) =
        withContext(Dispatchers.Default) {
            val urls = ArrayList<IgnoredEntry>()
            val words = ArrayList<IgnoredEntry>()
            for (entry in allEntries) {
                when (entry.type) {
                    IgnoredEntryType.URL -> urls.add(entry)
                    IgnoredEntryType.TEXT -> words.add(entry)
                }
            }
            urlEntries.postValue(urls)
            wordEntries.postValue(words)
        }

    private fun updateDisplayedList() = viewModelScope.launch(Dispatchers.Default) {
        displayedList.postValue(
            when (mode) {
                IgnoredEntryType.URL -> urlEntries.value
                IgnoredEntryType.TEXT -> wordEntries.value
            }
        )
    }

    /** リスト表示対象を切替え */
    fun setMode(mode: IgnoredEntryType, lifecycleOwner: LifecycleOwner) {
        this.mode = mode
        urlEntries.removeObserver(listObserver)
        wordEntries.removeObserver(listObserver)
        when (mode) {
            IgnoredEntryType.URL -> urlEntries.observe(lifecycleOwner, listObserver)
            IgnoredEntryType.TEXT -> wordEntries.observe(lifecycleOwner, listObserver)
        }
        if (this.mode == mode) return
        updateDisplayedList()
    }

    // ------ //

    fun add(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            repository.addIgnoredEntry(entry)
            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    fun delete(entry: IgnoredEntry) = viewModelScope.launch {
        runCatching {
            repository.deleteIgnoredEntry(entry)
        }
    }

    fun update(
        entry: IgnoredEntry,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            repository.updateIgnoredEntry(entry)
            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
    }

    // ------ //

    fun openMenuDialog(context: Context, entry: IgnoredEntry, fragmentManager: FragmentManager) {
        val items = arrayOf<Pair<String, Listener<AlertDialogFragment>>>(
            // 画面回転対策でfragmentManagerを明示的に渡している
            context.getString(R.string.pref_ignored_entries_menu_edit) to { f -> openModifyItemDialog(entry, f.parentFragmentManager) },
            context.getString(R.string.pref_ignored_entries_menu_remove) to { delete(entry) }
        )

        AlertDialogFragment.Builder()
            .setTitle("${entry.type.name} ${entry.query}")
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(items.map { it.first }) { f, which ->
                items[which].second.invoke(f)
            }
            .create()
            .show(fragmentManager, null)
    }

    fun openModifyItemDialog(entry: IgnoredEntry, fragmentManager: FragmentManager) {
        IgnoredEntryDialogFragment.createInstance(entry)
            .showAllowingStateLoss(fragmentManager, null)
    }

    fun openAddItemDialog(fragmentManager: FragmentManager) {
        val dummy = IgnoredEntry.createDummy(type = mode)
        IgnoredEntryDialogFragment.createInstance(dummy)
            .showAllowingStateLoss(fragmentManager, null)
    }
}
