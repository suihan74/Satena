package com.suihan74.satena.adapters

import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.BrowserToolbarManager
import com.suihan74.satena.R
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class EntriesAdapter(
    private val fragment : CoroutineScopeFragment,
    var category : Category,
    private val tabPosition: Int,
    private var entries : List<Entry>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = RecyclerState.makeStatesWithFooter(entries)

    private lateinit var singleTapAction : TapEntryAction
    private lateinit var longTapAction : TapEntryAction

    private val ignoredEntries = arrayListOf<IgnoredEntry>()

    var entireOffset : Int = entries.size
        private set

    init {
        refreshPreferences()
    }


    fun setEntries(e: List<Entry>) {
        states.clear()
        entireOffset = e.size
        entries = e.filterNot { entry -> ignoredEntries.any { it.isMatched(entry) } }
        states.run {
            addAll(entries.map { RecyclerState(RecyclerType.BODY, it) })
            add(RecyclerState(RecyclerType.FOOTER))
        }
        notifyDataSetChanged()
    }

    fun addEntries(e: List<Entry>) {
        val insertStartPosition = states.size - 1
        val newItems = e
            .filterNot { entry -> entries.any { it.id == entry.id } }
            .filterNot { entry -> ignoredEntries.any { it.isMatched(entry) } }
        entireOffset += e.size
        entries = entries.plus(newItems)
        states.addAll(states.size - 1, newItems.map { RecyclerState(RecyclerType.BODY, it) })
        notifyItemRangeInserted(insertStartPosition, newItems.size)
    }

    private fun refreshPreferences() {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(fragment.context)
        singleTapAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
        longTapAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))

        val ignoredEntriesPrefs = SafeSharedPreferences.create<IgnoredEntriesKey>(fragment.context)
        ignoredEntries.clear()
        ignoredEntriesPrefs.getObject<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES)?.let {
            ignoredEntries.addAll(it)
        }
    }

    fun onResume() {
        refreshPreferences()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_entries, parent, false)
                val holder = ViewHolder(inflate)

                holder.itemView.setOnClickListener {
                    val position = holder.adapterPosition
                    val entry = states[position].body!!
                    launchTappedAction(singleTapAction, entry)
                }

                holder.itemView.setOnLongClickListener {
                    val position = holder.adapterPosition
                    val entry = states[position].body!!
                    launchTappedAction(longTapAction, entry)
                    true
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

    private fun launchTappedAction(act: TapEntryAction, entry: Entry) {
        when (act) {
            TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(entry)
            TapEntryAction.SHOW_PAGE -> launchTabs(entry)
            TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(entry)
            TapEntryAction.SHOW_MENU -> showMenu(entry)
        }
    }

    private fun launchBookmarksActivity(entry: Entry) {
        val activity = fragment.activity ?: return
        val intent = Intent(activity, BookmarksActivity::class.java)
        intent.putExtra("entry", entry)
        activity.startActivity(intent)
    }

    private fun launchTabs(entry: Entry) {
        val context = fragment.context ?: return
        val url = entry.ampUrl ?: entry.url

        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .enableUrlBarHiding()
            .addDefaultShareMenuItem()
            .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSecondaryToolbarViews(
                BrowserToolbarManager.createRemoteViews(context, entry),
                BrowserToolbarManager.getClickableIds(),
                BrowserToolbarManager.getOnClickPendingIntent(context)
            )
            .build()

        intent.intent.setPackage("com.android.chrome")
        intent.launchUrl(context, Uri.parse(url))
    }

    private fun launchBrowser(entry: Entry) {
        val url = entry.ampUrl ?: entry.url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        fragment.activity!!.startActivity(intent)
    }

    private fun showMenu(entry: Entry) {
        val items = arrayListOf(
            TapEntryAction.SHOW_COMMENTS.title to { launchBookmarksActivity(entry) },
            TapEntryAction.SHOW_PAGE.title to { launchTabs(entry) },
            TapEntryAction.SHOW_PAGE_IN_BROWSER.title to { launchBrowser(entry) }
        )

        if (HatenaClient.signedIn()) {
            if (entry.bookmarkedData == null) {
                items.add("あとで読む" to { addToReadLaterEntries(entry) })
            }
            else {
                if (entry.bookmarkedData.tags.contains("あとで読む")) {
                    items.add("読んだ" to { bookmarkReadLaterEntry(entry) })
                }

                items.add("ブックマークを削除する" to { deleteBookmark(entry) })
            }
        }

        items.add("このサイトを非表示にする" to { addEntryToIgnores(entry) })

        AlertDialog.Builder(fragment.context!!, R.style.AlertDialogStyle)
            .setTitle(entry.title)
            .setNegativeButton("Cancel", null)
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                items[which].second()
            }
            .show()
    }

    private fun addToReadLaterEntries(entry: Entry) {
        fragment.launch(Dispatchers.Main) {
            val context = fragment.context ?: return@launch
            try {
                val result = HatenaClient.postBookmarkAsync(entry.url, readLater = true).await()
                val position: Int = entries.indexOfFirst { it.id == entry.id }
                states[position].body = updateEntry(entry,
                    bookmarkedData = result,
                    myhotentryComments = entry.myhotentryComments)

                notifyItemChanged(position)
                context.showToast("ブックマーク完了")
            }
            catch (e: Exception) {
                Log.d("FailedToBookmark", Log.getStackTraceString(e))
                context.showToast("ブックマーク失敗")
            }
        }
    }

    private fun bookmarkReadLaterEntry(entry: Entry) {
        fragment.launch(Dispatchers.Main) {
            val context = fragment.context ?: return@launch
            try {
                HatenaClient.postBookmarkAsync(entry.url, readLater = false).await()

                val tabType = EntriesTabType.fromCategory(category, tabPosition)
                if (EntriesTabType.READLATER == tabType) {
                    val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body == entry }
                    if (position >= 0) {
                        withContext(Dispatchers.Default) {
                            entries = entries.filterNot { it.id == entry.id }
                            states.removeAt(position)
                        }
                        notifyItemRemoved(position)
                    }
                }

                context.showToast("ブックマーク完了")
            }
            catch (e: Exception) {
                Log.d("FailedToBookmark", Log.getStackTraceString(e))
                context.showToast("ブックマーク失敗")
            }
        }
    }

    private fun addEntryToIgnores(entry: Entry) {
        val context = fragment.context ?: return

        if (ignoredEntries.any { it.type == IgnoredEntryType.URL && it.query == entry.url }) {
            return
        }

        val dialog = IgnoredEntryDialogFragment.createInstance(
            entry.url,
            entry.title
        ) { ignoredEntry ->
            if (ignoredEntries.contains(ignoredEntry)) {
                context.showToast("既に存在する非表示設定です")
                return@createInstance false
            }

            ignoredEntries.add(ignoredEntry)

            val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
            prefs.edit {
                putObject(IgnoredEntriesKey.IGNORED_ENTRIES, ignoredEntries)
            }

            setEntries(entries)
            context.showToast("${ignoredEntry.query} を非表示にしました")
            return@createInstance true
        }
        dialog.show(fragment.fragmentManager, "IgnoredEntryDialogFragment")
    }

    private fun deleteBookmark(entry: Entry) {
        fragment.launch(Dispatchers.Main) {
            val context = fragment.context ?: return@launch
            try {
                HatenaClient.deleteBookmarkAsync(entry.url).await()

                val position = states.indexOfFirst { it.type == RecyclerType.BODY && it.body == entry }
                if (position >= 0) {
                    if (Category.MyBookmarks == category) {
                        withContext(Dispatchers.Default) {
                            entries = entries.filterNot { it.id == entry.id }
                            states.removeAt(position)
                        }
                        notifyItemRemoved(position)
                    }
                    else {
                        states[position].body = updateEntry(entry,
                            bookmarkedData = null,
                            myhotentryComments = entry.myhotentryComments)
                        notifyItemChanged(position)
                    }
                }

                context.showToast("ブクマを削除しました")
            }
            catch (e: Exception) {
                Log.d("FailedToRemoveBookmark", Log.getStackTraceString(e))
                context.showToast("ブクマを削除できませんでした")
            }
        }
    }

    private fun updateEntry(
        src: Entry,
        id: Long? = null,
        title: String? = null,
        description: String? = null,
        count: Int? = null,
        url: String? = null,
        rootUrl: String? = null,
        faviconUrl: String? = null,
        imageUrl: String? = null,
        ampUrl: String? = null,
        bookmarkedData: BookmarkResult?,
        myhotentryComments: List<BookmarkResult>?
    ) = Entry(
            id = id ?: src.id,
            title = title ?: src.title,
            description = description ?: src.description,
            count = count ?: src.count,
            url = url ?: src.url,
            rootUrl = rootUrl ?: src.rootUrl ?: "",
            faviconUrl = faviconUrl ?: src.faviconUrl,
            imageUrl = imageUrl ?: src.imageUrl ?: "",
            ampUrl = ampUrl ?: src.ampUrl,
            bookmarkedData = bookmarkedData,
            myhotentryComments = myhotentryComments)

    private class ViewHolder(val root : View) : RecyclerView.ViewHolder(root) {
        private val title   = root.findViewById<TextView>(R.id.entry_title)!!
        private val domain  = root.findViewById<TextView>(R.id.entry_domain)!!
        private val count   = root.findViewById<TextView>(R.id.entry_bookmark_users)!!
        private val image   = root.findViewById<ImageView>(R.id.entry_image)!!
        private val favicon = root.findViewById<ImageView>(R.id.entry_favicon)!!

        private val commentsList = root.findViewById<RecyclerView>(R.id.comments_list)!!

        var entry : Entry? = null
            internal set(value) {
                field = value
                if (value == null) return

                title.text = value.title
                domain.text = Uri.parse(value.url).host
                count.text = "${value.count} users"

                if (value.faviconUrl.isNullOrEmpty()) {
                    favicon.visibility = View.GONE
                }
                else {
                    favicon.visibility = View.VISIBLE
                    Glide.with(root)
                        .load(value.faviconUrl)
                        .into(favicon)
                }

                Glide.with(root)
                    .load(value.imageUrl)
                    .into(image)

                val comments = ArrayList<BookmarkResult>()
                if (value.bookmarkedData != null) {
                    comments.add(value.bookmarkedData)
                }
                if (value.myhotentryComments != null) {
                    comments.addAll(value.myhotentryComments)
                }

                if (comments.isEmpty()) {
                    commentsList.visibility = View.GONE
                }
                else {
                    commentsList.apply {
                        visibility = View.VISIBLE
                        val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(root.context!!,
                            R.drawable.recycler_view_item_divider
                        )!!)
                        addItemDecoration(dividerItemDecoration)
                        layoutManager = LinearLayoutManager(commentsList.context)
                        adapter = object : EntryCommentsAdapter(comments) {
                            override fun onItemClicked(item: BookmarkResult) {
                                if (item.eid == null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.permalink))
                                    root.context.startActivity(intent)
                                }
                                else {
                                    val intent = Intent(root.context, BookmarksActivity::class.java)
                                    intent.putExtra("entry", value)
                                    intent.putExtra("target_user", item.user)
                                    root.context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
    }
}
