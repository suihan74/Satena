package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.DialogTitleEntry2Binding
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class EntryMenuDialog2 : DialogFragment() {
    companion object {
        fun createInstance(entry: Entry) = EntryMenuDialog2().withArguments {
            putObject(ARG_ENTRY, entry)
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
    }

    // ------ //

    private val viewModel by lazy {
        provideViewModel(this) {
            val entry = requireArguments().getObject<Entry>(ARG_ENTRY)!!
            val favoriteSitesRepo = FavoriteSitesRepository(
                SafeSharedPreferences.create(requireContext())
            )
            DialogViewModel(requireContext(), entry, favoriteSitesRepo)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        val entry = viewModel.entry

        val titleViewBinding = DataBindingUtil.inflate<DialogTitleEntry2Binding>(
            inflater,
            R.layout.dialog_title_entry2,
            null,
            false
        ).also {
            it.title = entry.title
            it.url = entry.url
            it.rootUrl = entry.rootUrl
            it.faviconUrl = entry.faviconUrl
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleViewBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setItems(viewModel.labels) { _, which ->
                viewModel.invokeAction(which)
            }
            .create()
    }

    // ------ //

    fun setShowCommentsListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showComments = l
    }

    fun setShowPageListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showPage = l
    }

    fun setSharePageListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.sharePage= l
    }

    fun setShowEntriesListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.showEntries = l
    }

    fun setFavoriteEntryListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.favorite = l
    }

    fun setUnfavoriteEntryListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.unfavorite = l
    }

    fun setIgnoreEntryListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.ignore = l
    }

    fun setReadLaterListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.readLater = l
    }

    fun setReadListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.read = l
    }

    fun setDeleteBookmarkListener(l : Listener<Entry>?) = lifecycleScope.launchWhenCreated {
        viewModel.deleteBookmark = l
    }

    // ------ //

    class DialogViewModel(
        val context: Context,
        val entry : Entry,
        val favoriteSitesRepo: FavoriteSitesRepository
    ) : ViewModel() {

        /** メニュー項目と対応する処理 */
        val items by lazy {
            createItems()
        }

        /** メニュー項目のラベル */
        val labels by lazy {
            items.map { context.getString(it.first) }.toTypedArray()
        }

        // ------ //

        /** ブクマ一覧画面を開く */
        var showComments : Listener<Entry>? = null

        /** ページを内部ブラウザで開く */
        var showPage : Listener<Entry>? = null

        /** ページを外部ブラウザで開く(ページを共有する) */
        var sharePage : Listener<Entry>? = null

        /** このサイトのエントリ一覧を開く */
        var showEntries : Listener<Entry>? = null

        /** お気に入りに追加 */
        var favorite : Listener<Entry>? = null

        /** お気に入りから除外 */
        var unfavorite : Listener<Entry>? = null

        /** 非表示設定を追加 */
        var ignore : Listener<Entry>? = null

        /** あとで読む */
        var readLater : Listener<Entry>? = null

        /** (あとで)読んだ */
        var read : Listener<Entry>? = null

        /** ブクマを削除する */
        var deleteBookmark : Listener<Entry>? = null

        // ------ //

        fun invokeAction(which: Int) {
            items[which].second?.invoke(entry)
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun createItems() = buildList {
            add(TapEntryAction.SHOW_COMMENTS.titleId to showComments)
            add(TapEntryAction.SHOW_PAGE.titleId to showPage)
            add(TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId to sharePage)
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
        }
    }
}
