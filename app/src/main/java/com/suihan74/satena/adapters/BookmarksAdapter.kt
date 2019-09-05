package com.suihan74.satena.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.*
import com.suihan74.satena.R
import com.suihan74.satena.models.*
import kotlinx.coroutines.*
import org.threeten.bp.format.DateTimeFormatter
import java.lang.StringBuilder

open class BookmarksAdapter(
    private val fragment: CoroutineScopeFragment,
    private val bookmarks : ArrayList<Bookmark>,
    private val bookmarksEntry: BookmarksEntry,
    private val starsMap: Map<String, StarsEntry>,
    private val tabType: BookmarksTabType
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states : ArrayList<RecyclerState<Bookmark>>
    var searchText = ""
        set(value) {
            field = value

            val list = bookmarks
                .filter { isShowable(it) }
                .map { RecyclerState(RecyclerType.BODY, it) }
            states.removeAll { it.type == RecyclerType.BODY }
            if (list.isNotEmpty()) {
                states.addAll(0, list)
            }

            notifyDataSetChanged()
        }

    private val showIgnoredUsersMention : Boolean
    private val showIgnoredUsersInAll : Boolean
    private val muteWords : List<String>

    init {
        val context = fragment.context!!
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        showIgnoredUsersMention = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
        showIgnoredUsersInAll = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS)

        val ignorePrefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        val ignoredEntries = ignorePrefs.get<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES)

        muteWords = ignoredEntries
            .filter { it.type == IgnoredEntryType.TEXT && it.target contains IgnoreTarget.BOOKMARK }
            .map { it.query }

        states = RecyclerState.makeStatesWithFooter(bookmarks.filter { bookmark ->
                muteWords.none { word ->
                    bookmark.comment.contains(word) && bookmark.getTagsText().contains(word)
                }
            })
    }

    private fun isShowable(b: Bookmark) =
        searchText.isBlank()
        || b.user.contains(searchText)
        || b.comment.contains(searchText)
        || b.tags.find { it.contains(searchText) } != null

    fun setBookmarks(b: List<Bookmark>) {
        if (bookmarks.isEmpty()) {
            bookmarks.addAll(b)
            for (i in 0 until bookmarks.size) {
                val item = bookmarks[i]
                if (isShowable(item)) {
                    states.add(i, RecyclerState(RecyclerType.BODY, item))
                    notifyItemInserted(i)
                }
            }
        }
        else {
            val indexed = b.mapIndexed { i, bookmark -> Pair(i, bookmark) }.toTypedArray()

            // 新規項目
            indexed
                .filter {
                    val target = it.second
                    bookmarks.find { b -> b.user == target.user } == null
                }
                .sortedBy { it.first }
                .forEach {
                    val item = it.second
                    bookmarks.add(it.first, item)
                    if (isShowable(item)) {
                        states.add(it.first, RecyclerState(RecyclerType.BODY, item))
                        notifyItemInserted(it.first)
                    }
                }

            // 内容の更新
            indexed
                .filter {
                    val target = it.second
                    bookmarks.firstOrNull { b -> b.user == target.user && b.comment != target.comment } != null
                }
                .forEach {
                    val item = it.second
                    bookmarks[it.first] = item
                    if (isShowable(item)) {
                        states[it.first] = RecyclerState(RecyclerType.BODY, item)
                        notifyItemChanged(it.first)
                    }
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_bookmarks, parent, false)
                return ViewHolder(
                    inflate,
                    fragment,
                    showIgnoredUsersMention,
                    showIgnoredUsersInAll && tabType == BookmarksTabType.ALL
                ).apply {

                    itemView.setOnClickListener {
                        val bookmark = states[adapterPosition].body!!
                        onItemClicked(bookmark)
                    }

                    itemView.setOnLongClickListener {
                        val bookmark = states[adapterPosition].body!!
                        onItemLongClicked(bookmark)
                    }
                }
            }

            RecyclerType.FOOTER -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                return FooterViewHolder(inflate)
            }

            else -> throw RuntimeException("invalid RecyclerState")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).apply {
                    val b = states[position].body!!
                    setBookmark(b, bookmarksEntry)
                    starsEntry = this@BookmarksAdapter.starsMap[b.user]
                }
            }

            else -> {}
        }
    }

    override fun getItemCount() = states.size
    override fun getItemViewType(position: Int): Int = states[position].type.int
    fun getItemPosition(user: String) : Int = states.indexOfFirst { it.type == RecyclerType.BODY && it.body!!.user == user }

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) : Boolean = true

    fun removeItem(bookmark: Bookmark) {
        val position = getItemPosition(bookmark.user)
        states.removeAt(position)
        notifyItemRemoved(position)
    }

    fun notifyItemChanged(bookmark: Bookmark) {
        val position = getItemPosition(bookmark.user)
        notifyItemChanged(position)
    }


    class ViewHolder(
        private val view : View,
        private val fragment : CoroutineScopeFragment,
        private val showIgnoredUsersMention : Boolean,
        private val showIgnoredUsersInAll : Boolean
    ) : RecyclerView.ViewHolder(view) {

        private val userName    = view.findViewById<TextView>(R.id.bookmark_user_name)!!
        private val userIcon    = view.findViewById<ImageView>(R.id.bookmark_user_icon)!!
        private val comment     = view.findViewById<TextView>(R.id.bookmark_comment)!!
        private val tags        = view.findViewById<TextView>(R.id.bookmark_tags)!!
        private val tagsLayout  = view.findViewById<ViewGroup>(R.id.bookmark_tags_layout)!!
        private val timestamp   = view.findViewById<TextView>(R.id.bookmark_timestamp)!!
        private val ignoredMark = view.findViewById<ImageView>(R.id.ignored_user_mark)!!

        private val mentionLayout   = view.findViewById<RelativeLayout>(R.id.bookmark_mention_area)!!
        private val mentionUserIcon = view.findViewById<ImageView>(R.id.bookmark_mention_user_icon)!!
        private val mentionUserName = view.findViewById<TextView>(R.id.bookmark_mention_user_name)!!
        private val mentionComment  = view.findViewById<TextView>(R.id.bookmark_mention_comment)!!

        private var bookmark : Bookmark? = null

        fun setBookmark(bookmark : Bookmark, bookmarksEntry : BookmarksEntry) {
            this.bookmark = bookmark

            if (HatenaClient.ignoredUsers.contains(bookmark.user)) {
                ignoredMark.visibility = View.VISIBLE
            }
            else {
                ignoredMark.visibility = View.GONE
            }

            val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
            userName.text = bookmark.user
            comment.apply {
                text = analyzed.comment

                visibility = if (text.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

                setOnTouchListener { view, event ->
                    val textView = view as TextView
                    val m = MutableLinkMovementMethod { url ->
                        if (url.startsWith("http")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        else {
                            val eid = analyzed.entryIds.firstOrNull { url.contains(it.toString()) }
                            if (eid != null) {
                                fragment.launch(Dispatchers.Main) {
                                    val entryUrl = HatenaClient.getEntryUrlFromIdAsync(eid).await()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entryUrl))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                    textView.movementMethod = m
                    val mTouched = m.onTouchEvent(textView, textView.text as Spannable, event)
                    textView.movementMethod = null
                    textView.isFocusable = false

                    return@setOnTouchListener mTouched
                }
            }

            tags.text = bookmark.getTagsText()
            if (tags.text.isEmpty()) {
                tagsLayout.visibility = View.GONE
            }
            else {
                tagsLayout.visibility = View.VISIBLE
            }

            val builder = StringBuilder()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            builder.append(bookmark.timestamp.format(formatter))
            builder.append("　")

            if (bookmark.starCount != null) {
                val yellowStarCount = bookmark.starCount.firstOrNull { it.color == StarColor.Yellow }?.count ?: 0
                val redStarCount = bookmark.starCount.firstOrNull { it.color == StarColor.Red }?.count ?: 0
                val greenStarCount = bookmark.starCount.firstOrNull { it.color == StarColor.Green }?.count ?: 0
                val blueStarCount = bookmark.starCount.firstOrNull { it.color == StarColor.Blue }?.count ?: 0
                val purpleStarCount = bookmark.starCount.firstOrNull { it.color == StarColor.Purple }?.count ?: 0

                appendStarText(builder, purpleStarCount, view.context, R.color.starPurple)
                appendStarText(builder, blueStarCount, view.context, R.color.starBlue)
                appendStarText(builder, redStarCount, view.context, R.color.starRed)
                appendStarText(builder, greenStarCount, view.context, R.color.starGreen)
                appendStarText(builder, yellowStarCount, view.context, R.color.starYellow)
            }
            timestamp.setHtml(builder.toString())

            Glide.with(view)
                .load(bookmark.userIconUrl)
                .into(userIcon)

            mentionLayout.visibility = View.GONE
            if (analyzed.ids.isNotEmpty()) {
                val mentionUser = analyzed.ids.first()

                if (showIgnoredUsersMention ||
                    showIgnoredUsersInAll ||
                    !HatenaClient.ignoredUsers.contains(mentionUser)
                ) {
                    val mentionBookmark = bookmarksEntry.bookmarks.firstOrNull { it.user == mentionUser }
                    if (mentionBookmark != null) {
                        mentionLayout.visibility = View.VISIBLE
                        mentionUserName.text = mentionBookmark.user
                        mentionComment.text = BookmarkCommentDecorator.convert(mentionBookmark.comment).comment
                        Glide.with(view)
                            .load(mentionBookmark.userIconUrl)
                            .into(mentionUserIcon)
                    }
                }
            }
        }

        var starsEntry : StarsEntry? = null
            get
            internal set(value) {
                field = value

                val builder = StringBuilder()
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                builder.append(bookmark!!.timestamp.format(formatter))
                builder.append("　")

                if (value != null) {
                    val yellowStarCount = value.getStarsCount(StarColor.Yellow)
                    val redStarCount = value.getStarsCount(StarColor.Red)
                    val greenStarCount = value.getStarsCount(StarColor.Green)
                    val blueStarCount = value.getStarsCount(StarColor.Blue)
                    val purpleStarCount = value.getStarsCount(StarColor.Purple)

                    appendStarText(builder, purpleStarCount, view.context, R.color.starPurple)
                    appendStarText(builder, blueStarCount, view.context, R.color.starBlue)
                    appendStarText(builder, redStarCount, view.context, R.color.starRed)
                    appendStarText(builder, greenStarCount, view.context, R.color.starGreen)
                    appendStarText(builder, yellowStarCount, view.context, R.color.starYellow)
                }
                timestamp.setHtml(builder.toString())
            }

        private fun appendStarText(builder: StringBuilder, count: Int, context: Context, colorId: Int) {
            val color = ContextCompat.getColor(context, colorId)
            builder.append("<font color=\"$color\">")
            if (count > 10) {
                builder.append("★$count")
            }
            else {
                for (i in 1..count) builder.append("★")
            }
            builder.append("</font>")
        }
    }
}
