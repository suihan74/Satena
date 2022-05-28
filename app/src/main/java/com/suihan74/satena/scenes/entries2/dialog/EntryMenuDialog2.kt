package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.databinding.FragmentDialogEntryMenuBinding
import com.suihan74.satena.databinding.ListviewItemDialogEntryMenuReadBinding
import com.suihan74.satena.databinding.ListviewItemDialogMenuBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.ReadEntriesRepository
import com.suihan74.satena.scenes.preferences.createLiveData
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class EntryMenuItem(
    @StringRes
    val first : Int,
    val second : DialogListener<Entry>?
)

class EntryMenuDialog2 : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = EntryMenuDialog2().withArguments {
            putObject(ARG_ENTRY, entry)
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        val app = SatenaApplication.instance
        val entry = requireArguments().getObject<Entry>(ARG_ENTRY)!!
        val prefs = SafeSharedPreferences.create<PreferenceKey>(app)
        DialogViewModel(
            entry,
            prefs,
            app.favoriteSitesRepository,
            app.readEntriesRepository
        )
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val titleViewBinding = DialogTitleEntry2Binding.inflate(inflater, null, false).also {
            viewModel.entry.let { entry ->
                it.title = entry.title
                it.url = entry.url
                it.rootUrl = entry.rootUrl
                it.faviconUrl = entry.faviconUrl
            }
        }

        val contentViewBinding = FragmentDialogEntryMenuBinding.inflate(inflater, null, false).also { binding ->
            binding.recyclerView.adapter = MenuItemsAdapter(this, viewModel).also { adapter ->
                adapter.submitList(viewModel.createItems(requireActivity()))
            }
        }

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setView(contentViewBinding.root)
            .create()
    }

    // ------ //

    fun setShowCommentsListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showComments = l
    }

    fun setShowPageListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showPage = l
    }

    fun setShowPageInBrowserListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showPageInBrowser = l
    }

    fun setSharePageListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.sharePage = l
    }

    fun setShowEntriesListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showEntries = l
    }

    fun setFavoriteEntryListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.favorite = l
    }

    fun setUnfavoriteEntryListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.unfavorite = l
    }

    fun setIgnoreEntryListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.ignore = l
    }

    fun setIgnoreAdsListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.ignoreAds = l
    }

    fun setReadLaterListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.readLater = l
    }

    fun setReadListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.read = l
    }

    fun setDeleteBookmarkListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.deleteBookmark = l
    }

    fun setDeleteReadMarkListener(l: DialogListener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.deleteReadMark = l
    }

    // ------ //

    class DialogViewModel(
        val entry : Entry,
        prefs : SafeSharedPreferences<PreferenceKey>,
        private val favoriteSitesRepo : FavoriteSitesRepository,
        private val readEntriesRepo : ReadEntriesRepository
    ) : ViewModel() {
        /** ブクマ一覧画面を開く */
        var showComments : DialogListener<Entry>? = null

        /** ページを内部ブラウザで開く */
        var showPage : DialogListener<Entry>? = null

        /** ページを外部ブラウザで開く */
        var showPageInBrowser : DialogListener<Entry>? = null

        /** ページを外部ブラウザで開く(ページを共有する) */
        var sharePage : DialogListener<Entry>? = null

        /** このサイトのエントリ一覧を開く */
        var showEntries : DialogListener<Entry>? = null

        /** お気に入りに追加 */
        var favorite : DialogListener<Entry>? = null

        /** お気に入りから除外 */
        var unfavorite : DialogListener<Entry>? = null

        /** 非表示設定を追加 */
        var ignore : DialogListener<Entry>? = null

        /** 広告を非表示にする */
        var ignoreAds : DialogListener<Entry>? = null

        /** あとで読む */
        var readLater : DialogListener<Entry>? = null

        /** (あとで)読んだ */
        var read : DialogListener<Entry>? = null

        /** ブクマを削除する */
        var deleteBookmark : DialogListener<Entry>? = null

        /** 既読マークを削除する */
        var deleteReadMark : DialogListener<Entry>? = null

        // ------ //

        val privateReadLater = createLiveData<PreferenceKey, Boolean>(prefs, PreferenceKey.ENTRY_PRIVATE_READ_LATER)
        val privateReadLaterTooltip = privateReadLater.asFlow().map {
            if (it) R.string.hint_private_toggle_on
            else R.string.hint_private_toggle_off
        }.asLiveData()

        // ------ //

        fun createItems(activity: FragmentActivity) = buildList<EntryMenuItem> {
            add(TapEntryAction.SHOW_COMMENTS.textId, showComments)
            add(TapEntryAction.SHOW_PAGE.textId, showPage)
            add(TapEntryAction.SHOW_PAGE_IN_BROWSER.textId, showPageInBrowser)
            add(TapEntryAction.SHARE.textId, sharePage)
            add(R.string.entry_action_show_entries, showEntries)

            val alreadyFavorite =
                runBlocking {
                    favoriteSitesRepo.contains(entry.rootUrl) || favoriteSitesRepo.contains(entry.url)
                }

            if (alreadyFavorite) {
                add(R.string.entry_action_unfavorite, unfavorite)
            }
            else {
                add(R.string.entry_action_favorite, favorite)
            }

            add(R.string.entry_action_ignore, ignore)
            if (entry.adUrl != null) {
                add(R.string.entry_action_ignore_ads, ignoreAds)
            }

            if (HatenaClient.signedIn() && entry.bookmarkedData == null) {
                add(R.string.entry_action_read_later, readLater)
            }

            if (HatenaClient.signedIn() && entry.bookmarkedData?.tags?.contains("あとで読む") == true) {
                add(R.string.entry_action_read, read)
            }

            if (entry.bookmarkedData != null) {
                add(R.string.entry_action_delete_bookmark, deleteBookmark)
            }

            if (activity is EntriesActivity && readEntriesRepo.readEntryIds.value.contains(entry.id)) {
                add(R.string.entries_delete_read_mark_desc, deleteReadMark)
            }
        }

        private fun MutableList<EntryMenuItem>.add(textId: Int, listener: DialogListener<Entry>?) {
            add(EntryMenuItem(textId, listener))
        }
    }
}

class MenuItemsAdapter(
    private val dialogFragment: DialogFragment,
    private val viewModel: EntryMenuDialog2.DialogViewModel
) : ListAdapter<EntryMenuItem, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun getItemViewType(position: Int) = currentList[position].first

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.string.entry_action_read_later,
            R.string.entry_action_read ->
                MenuItemReadViewHolder(
                    ListviewItemDialogEntryMenuReadBinding.inflate(layoutInflater, parent, false)
                )

            else ->
                MenuItemViewHolder(
                    ListviewItemDialogMenuBinding.inflate(layoutInflater, parent, false)
                )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.alsoAs<MenuItemViewHolder> {
            currentList[position].let { item ->
                it.binding.item = item
                it.binding.root.setOnClickListener {
                    item.second?.invoke(viewModel.entry, dialogFragment)
                    runCatching { dialogFragment.dismiss() }
                }
                it.binding.lifecycleOwner = dialogFragment
            }
        }
        holder.alsoAs<MenuItemReadViewHolder> {
            currentList[position].let { item ->
                it.binding.item = item
                it.binding.root.setOnClickListener {
                    item.second?.invoke(viewModel.entry, dialogFragment)
                    runCatching { dialogFragment.dismiss() }
                }
                it.binding.vm = viewModel
                it.binding.lifecycleOwner = dialogFragment
            }
        }
    }

    // ------ //

    class MenuItemViewHolder(val binding: ListviewItemDialogMenuBinding) : RecyclerView.ViewHolder(binding.root)
    class MenuItemReadViewHolder(val binding: ListviewItemDialogEntryMenuReadBinding) : RecyclerView.ViewHolder(binding.root)

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<EntryMenuItem>() {
        override fun areItemsTheSame(oldItem: EntryMenuItem, newItem: EntryMenuItem): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: EntryMenuItem, newItem: EntryMenuItem): Boolean {
            return oldItem.second?.equals(newItem.second) == true
        }
    }
}
