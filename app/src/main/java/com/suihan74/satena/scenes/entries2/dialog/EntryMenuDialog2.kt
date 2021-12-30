package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.ReadEntriesRepository
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.lazyProvideViewModel

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
        DialogViewModel(
            entry,
            app.favoriteSitesRepository,
            app.readEntriesRepository
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val entry = viewModel.entry
        val titleViewBinding = DialogTitleEntry2Binding.inflate(inflater, null, false).also {
            it.title = entry.title
            it.url = entry.url
            it.rootUrl = entry.rootUrl
            it.faviconUrl = entry.faviconUrl
        }

        return createBuilder()
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(viewModel.labels(requireActivity())) { _, which ->
                viewModel.invokeAction(which, this)
            }
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
        private val favoriteSitesRepo : FavoriteSitesRepository,
        private val readEntriesRepo : ReadEntriesRepository
    ) : ViewModel() {

        /** メニュー項目と対応する処理 */
        private var items : List<Pair<Int, DialogListener<Entry>?>> = emptyList()

        /** メニュー項目のラベル */
        fun labels(activity: FragmentActivity) : Array<String> =
            createItems(activity)
                .also { this.items = it }
                .map { activity.getString(it.first) }
                .toTypedArray()

        // ------ //

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

        /** あとで読む */
        var readLater : DialogListener<Entry>? = null

        /** (あとで)読んだ */
        var read : DialogListener<Entry>? = null

        /** ブクマを削除する */
        var deleteBookmark : DialogListener<Entry>? = null

        /** 既読マークを削除する */
        var deleteReadMark : DialogListener<Entry>? = null

        // ------ //

        fun invokeAction(which: Int, fragment: EntryMenuDialog2) {
            items[which].second?.invoke(entry, fragment)
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun createItems(activity: FragmentActivity) = buildList {
            add(TapEntryAction.SHOW_COMMENTS.textId to showComments)
            add(TapEntryAction.SHOW_PAGE.textId to showPage)
            add(TapEntryAction.SHOW_PAGE_IN_BROWSER.textId to showPageInBrowser)
            add(TapEntryAction.SHARE.textId to sharePage)
            add(R.string.entry_action_show_entries to showEntries)

            val alreadyFavorite = favoriteSitesRepo.favoriteSites.value?.any {
                it.url == entry.rootUrl || it.url == entry.url
            } != false
            if (alreadyFavorite) {
                add(R.string.entry_action_unfavorite to unfavorite)
            }
            else {
                add(R.string.entry_action_favorite to favorite)
            }

            add(R.string.entry_action_ignore to ignore)

            if (HatenaClient.signedIn() && entry.bookmarkedData == null) {
                add(R.string.entry_action_read_later to readLater)
            }

            if (HatenaClient.signedIn() && entry.bookmarkedData?.tags?.contains("あとで読む") == true) {
                add(R.string.entry_action_read to read)
            }

            if (entry.bookmarkedData != null) {
                add(R.string.entry_action_delete_bookmark to deleteBookmark)
            }

            if (activity is EntriesActivity && readEntriesRepo.readEntryIds.value.contains(entry.id)) {
                add(R.string.entries_delete_read_mark_desc to deleteReadMark)
            }
        }
    }
}
