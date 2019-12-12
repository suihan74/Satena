package com.suihan74.satena.scenes.bookmarks

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.TappedActionLauncher
import com.suihan74.satena.models.*
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

open class BookmarksAdapter(
    private val fragment: BookmarksTabFragment,
    bookmarks : List<Bookmark>,
    private val bookmarksEntry: BookmarksEntry,
    private val tabType: BookmarksTabType
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mStates : ArrayList<RecyclerState<Bookmark>>

    private val showIgnoredUsersMention : Boolean
    private val showIgnoredUsersInAll : Boolean
    private val muteWords : List<String>

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

    private fun isShowable(b: Bookmark) : Boolean {
        val tags = fragment.taggedUsers?.firstOrNull { it.user.name == b.user }?.tags ?: emptyList()

        return searchText.isBlank()
                || b.user.contains(searchText)
                || b.comment.contains(searchText)
                || b.tags.find { it.contains(searchText) } != null
                || tags.any { it.name.contains(searchText) }
    }

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
                        state.body!!.user == it.value.user
                    }
                    Pair(existedIndex, it.value)
                }
                .filter { (existedIndex, item) ->
                    if (existedIndex < 0) return@filter false
                    val existed = mStates[existedIndex].body!!

                    existed.comment != item.comment ||
                    existed.getTagsText() != item.getTagsText() ||
                    !equalsStarCount(existed.starCount, item.starCount)
                }
                .forEach { (existedIndex, item) ->
                    if (isShowable(item)) {
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
                    this,
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

    val bookmarksCount : Int
        get() = states.count { it.type == RecyclerType.BODY }

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) : Boolean = true

    fun removeItem(bookmark: Bookmark) {
        val position = getItemPosition(bookmark.user)
        if (position >= 0) {
            val item = states[position]
            val actualPosition = mStates.indexOf(item)
            mStates.removeAt(actualPosition)
            notifyItemRemoved(position)
        }
    }

    fun notifyItemChanged(bookmark: Bookmark) {
        val position = getItemPosition(bookmark.user)
        if (position >= 0) {
            notifyItemChanged(position)
        }

        // メンション表示も更新する
        fragment.launch(Dispatchers.Default) {
            states.filter { it.type == RecyclerType.BODY }
                .filter {
                    BookmarkCommentDecorator.convert(it.body!!.comment).ids.contains(bookmark.user)
                }
                .forEach {
                    withContext(Dispatchers.Main) {
                        val pos = getItemPosition(it.body!!.user)
                        if (pos >= 0) {
                            notifyItemChanged(pos)
                        }
                    }
                }
        }
    }


    class ViewHolder(
        private val view : View,
        private val adapter: BookmarksAdapter,
        private val fragment : BookmarksTabFragment,
        private val showIgnoredUsersMention : Boolean,
        private val showIgnoredUsersInAll : Boolean
    ) : RecyclerView.ViewHolder(view) {

        private val userName    = view.findViewById<TextView>(R.id.bookmark_user_name)!!
        private val userIcon    = view.findViewById<ImageView>(R.id.bookmark_user_icon)!!
        private val comment     = view.findViewById<TextView>(R.id.bookmark_comment)!!
        private val tags        = view.findViewById<TextView>(R.id.bookmark_tags)!!
        private val timestamp   = view.findViewById<TextView>(R.id.bookmark_timestamp)!!
        private val ignoredMark = view.findViewById<ImageView>(R.id.ignored_user_mark)!!

        private val mentionsList = view.findViewById<RecyclerView>(R.id.bookmark_mentions)!!
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

                val linkMovementMethod = object : MutableLinkMovementMethod() {
                    override fun onSinglePressed(link: String) {
                        if (link.startsWith("http")) {
                            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                            val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
                            TappedActionLauncher.launch(context, act, link, fragment)
                        }
                        else {
                            val eid = analyzed.entryIds.firstOrNull { link.contains(it.toString()) }
                            if (eid != null) {
                                fragment.launch(Dispatchers.Main) {
                                    val entryUrl = HatenaClient.getEntryUrlFromIdAsync(eid).await()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entryUrl))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }

                    override fun onLongPressed(link: String) {
                        if (link.startsWith("http")) {
                            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                            val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
                            TappedActionLauncher.launch(context, act, link, fragment)
                        }
                    }
                }

                setOnTouchListener { view, event ->
                    val textView = view as TextView
                    return@setOnTouchListener linkMovementMethod.onTouchEvent(
                        textView,
                        SpannableString(textView.text),
                        event)
                }
            }

            // タグの表示
            tags.apply {
                val tagsText = bookmark.getTagsText()
                if (tagsText.isEmpty()) {
                    visibility = View.GONE
                }
                else {
                    visibility = View.VISIBLE
                    text = SpannableString("_$tagsText").apply {
                        val icon = resources.getDrawable(R.drawable.ic_tag, null).apply {
                            setBounds(0, 0, lineHeight, lineHeight)
                            setTint(resources.getColor(R.color.tagColor, null))
                        }
                        setSpan(ImageSpan(icon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            // ユーザーにつけられたタグの表示
            val userTagsText = view.findViewById<TextView>(R.id.user_tags)
            val user = bookmark.user
            val tags = fragment.taggedUsers?.firstOrNull { it.user.name == user }?.tags?.sortedBy { it.id }
            if (tags.isNullOrEmpty()) {
                userTagsText.visibility = View.GONE
            }
            else {
                userTagsText.apply {
                    val icon = resources.getDrawable(R.drawable.ic_user_tag, null).apply {
                        val size = textSize.toInt()
                        setBounds(0, 0, size, size)
                        setTint(resources.getColor(R.color.tagColor, null))
                    }
                    setCompoundDrawablesRelative(icon, null, null, null)

                    text = tags.joinToString(", ") { it.name }
                    visibility = View.VISIBLE
                }
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

            val mentions = analyzed.ids.mapNotNull { userId ->
                if (showIgnoredUsersMention
                    || showIgnoredUsersInAll
                    || !HatenaClient.ignoredUsers.contains(userId)
                ) {
                    bookmarksEntry.bookmarks.firstOrNull { it.user == userId }
                }
                else null
            }

            // 言及先リスト
            mentionsList.apply {
                if (mentions.isEmpty()) {
                    visibility = View.GONE
                    adapter = null
                }
                else {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context)
                    adapter = object : MentionsAdapter(mentions) {
                        override fun onItemClicked(item: Bookmark) {
                            this@ViewHolder.adapter.onItemClicked(item)
                        }

                        override fun onItemLongClicked(item: Bookmark): Boolean {
                            this@ViewHolder.adapter.onItemLongClicked(item)
                            return true
                        }
                    }

                    repeat(itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }

                    val dividerItemDecoration = DividerItemDecorator(
                        ContextCompat.getDrawable(context, R.drawable.recycler_view_item_divider)!!)
                    addItemDecoration(dividerItemDecoration)
                }
            }
        }
    }
}
