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
import com.suihan74.satena.R
import com.suihan74.satena.models.*
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

open class BookmarksAdapter(
    private val fragment: CoroutineScopeFragment,
    bookmarks : List<Bookmark>,
    private val bookmarksEntry: BookmarksEntry,
    private val tabType: BookmarksTabType
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mStates : ArrayList<RecyclerState<Bookmark>>

    var loadableFooter : LoadableFooterViewHolder? = null
        private set

    private val states : List<RecyclerState<Bookmark>>
        get() =
            if (searchText.isBlank()) {
                mStates
            }
            else {
                mStates.filter { RecyclerType.BODY != it.type || isShowable(it.body!!) }
            }

    var searchText = ""
        set(value) {
            field = value

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

        mStates = RecyclerState.makeStatesWithFooter(bookmarks.filter { bookmark ->
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

    private fun getStarCount(list: List<Star>, color: StarColor) =
        list.firstOrNull { it.color == color }?.count ?: 0

    private fun equalsStarCount(prev: List<Star>?, cur: List<Star>?) : Boolean {
        val isPrevEmpty = prev.isNullOrEmpty()
        val isCurEmpty = cur.isNullOrEmpty()
        if (isPrevEmpty != isCurEmpty) return true
        return if (!isPrevEmpty && !isCurEmpty) {
            prev!!.size != cur!!.size || StarColor.values().all { getStarCount(prev, it) == getStarCount(cur, it) }
        }
        else true
    }

    fun setBookmarks(bookmarks: List<Bookmark>) {
        if (RecyclerState.isBodiesEmpty(mStates)) {
            mStates = RecyclerState.makeStatesWithFooter(bookmarks)
            notifyDataSetChanged()
        }
        else {
            val indexed = bookmarks.withIndex()

            // 新規項目
            indexed
                .filterNot {
                    mStates.any { st -> st.body?.user == it.value.user }
                }
                .forEach { (_, item) ->
                    if (isShowable(item)) {
                        val actualPosition = mStates.indexOfFirst { item.timestamp > it.body?.timestamp ?: LocalDateTime.MIN }
                        val state = RecyclerState(RecyclerType.BODY, item)
                        mStates.add(actualPosition, state)

                        val position = states.indexOfFirst { it == state }
                        notifyItemInserted(position)
                    }
                }

            // 内容の更新
            indexed
                .map {
                    val existedIndex = mStates.indexOfFirst { state ->
                        RecyclerType.BODY == state.type &&
                        state.body!!.user == it.value.user &&
                        (state.body!!.comment != it.value.comment ||
                        state.body!!.getTagsText() != it.value.getTagsText() ||
                        !equalsStarCount(state.body!!.starCount, it.value.starCount))
                    }
                    Pair(existedIndex, it.value)
                }
                .forEach { (existedIndex, item) ->
                    if (existedIndex >= 0 && isShowable(item)) {
                        mStates[existedIndex] = RecyclerState(RecyclerType.BODY, item)

                        val position = states.indexOfFirst { it == mStates[existedIndex] }
                        notifyItemChanged(position)
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
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view_loadable, parent, false)
                loadableFooter = LoadableFooterViewHolder(inflate)
                return loadableFooter!!
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
        val item = states[position]
        val actualPosition = mStates.indexOf(item)
        mStates.removeAt(actualPosition)
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
