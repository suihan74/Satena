package com.suihan74.satena.scenes.bookmarks2

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.WorkerThread
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.StarColor
import com.suihan74.hatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.bookmarks2.tab.BookmarksTabViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.bindings.setVisibility
import kotlinx.android.synthetic.main.listview_item_bookmarks.view.*
import org.threeten.bp.format.DateTimeFormatter

fun <T> List<T>?.contentsEquals(other: List<T>?) =
    if (this == null && other == null) true
    else if (this == null && other != null) false
    else if (this != null && other == null) false
    else this!!.size == other!!.size && this.mapIndexed { index, _ -> this[index] == other[index] }.all { it }


open class BookmarksAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: BookmarksTabViewModel
) : ListAdapter<RecyclerState<BookmarksAdapter.Entity>, RecyclerView.ViewHolder>(DiffCallback()) {
    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<Entity>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<Entity>,
            newItem: RecyclerState<Entity>
        ): Boolean {
            return oldItem.type == newItem.type && oldItem.body?.bookmark?.user == newItem.body?.bookmark?.user
        }

        override fun areContentsTheSame(
            oldItem: RecyclerState<Entity>,
            newItem: RecyclerState<Entity>
        ): Boolean {
            return oldItem.type == newItem.type && oldItem.body?.equals(newItem.body) == true
        }
    }

    data class Entity (
        val bookmark: Bookmark,
        val analyzedComment: AnalyzedBookmarkComment,
        val isIgnored: Boolean,
        val mentions: List<Bookmark>,
        val userTags: List<Tag>
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is Entity) return false

            return bookmark.same(other.bookmark) &&
                    isIgnored == other.isIgnored &&
                    mentions.contentsEquals(other.mentions) &&
                    userTags.contentsEquals(other.userTags)
        }

        override fun hashCode() = super.hashCode()
    }

    /** 表示項目リスト */
    private var loadableFooter: LoadableFooterViewHolder? = null

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) = false

    /** コメント中のリンクをタップしたときの処理 */
    open fun onLinkClicked(url: String) {}
    /** コメント中のリンクをロングタップしたときの処理 */
    open fun onLinkLongClicked(url: String) {}
    /** コメント中のEntryIdをタップしたときの処理 */
    open fun onEntryIdClicked(eid: Long) {}

    /** フッターの追加ロードをタップしたときの処理 */
    open fun onAdditionalLoading() {}

    /** 追加ロードボタンを表示するか */
    var additionalLoadable: Boolean = false
        set (value) {
            field = value
            loadableFooter?.additionalLoadingTextView?.setVisibility(value)
        }

    override fun getItemCount() = currentList.size

    override fun getItemViewType(position: Int) =
        currentList[position].type.int

    /**
     * 指定ブクマの位置を取得する
     * @return 対象が存在するとき ---> そのインデックス
     *
     * 対象が存在しないとき ---> -1
     */
    fun getPosition(bookmark: Bookmark) =
        currentList.indexOfFirst { it.body?.bookmark?.user == bookmark.user }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(
                        inflater.inflate(R.layout.listview_item_bookmarks, parent, false),
                        this
                    ).apply {
                        itemView.setOnClickListener {
                            val bookmark = currentList[adapterPosition].body!!.bookmark
                            onItemClicked(bookmark)
                        }
                        itemView.setOnLongClickListener {
                            val bookmark = currentList[adapterPosition].body!!.bookmark
                            onItemLongClicked(bookmark)
                        }
                    }

                RecyclerType.FOOTER ->
                    LoadableFooterViewHolder(
                        inflater.inflate(R.layout.footer_recycler_view_loadable, parent, false)
                    ).also {
                        loadableFooter = it.also { footer ->
                            stopLoading()
                            footer.additionalLoadingTextView?.let { textView ->
                                textView.setOnClickListener {
                                    onAdditionalLoading()
                                }
                                textView.setVisibility(additionalLoadable)
                            }
                        }
                    }

                else -> throw RuntimeException("an invalid list item")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).run {
                    val entity = currentList[position].body!!
                    bookmark = entity
                }
            }

            else -> Unit
        }

    /** フッタのローディングアニメを表示する */
    fun startLoading() {
        additionalLoadable = false
        loadableFooter?.showProgressBar()
    }

    /** フッタのローディングアニメを隠す */
    fun stopLoading(additionalLoadable: Boolean = this.additionalLoadable) {
        this.additionalLoadable = additionalLoadable
        loadableFooter?.hideProgressBar(additionalLoadable)
    }

    fun setBookmarks(
        bookmarks: List<Bookmark>,
        bookmarksEntry: BookmarksEntry,
        taggedUsers: List<UserAndTags>,
        ignoredUsers: List<String>,
        displayMutedMention: Boolean
    ) {
        val newStates = RecyclerState.makeStatesWithFooter(bookmarks.map {
            val analyzedComment = BookmarkCommentDecorator.convert(it.comment)
            Entity(
                bookmark = it,
                analyzedComment = analyzedComment,
                isIgnored = ignoredUsers.contains(it.user),
                mentions = analyzedComment.ids.mapNotNull { called ->
                    bookmarksEntry.bookmarks.firstOrNull { b -> b.user == called }?.let { mentioned ->
                        if (!displayMutedMention && ignoredUsers.contains(mentioned.user)) null
                        else mentioned
                    }
                },
                userTags = taggedUsers.firstOrNull { t -> t.user.name == it.user }?.tags ?: emptyList()
            )
        })

        submitList(newStates)
    }

    /** スター情報を更新 */
    @WorkerThread
    fun updateStar(starEntries: List<StarsEntry>?) {
        if (starEntries == null) return
        var updated = false
        val newStates = RecyclerState.makeStatesWithFooter(
            currentList.mapNotNull { if (it.type == RecyclerType.BODY) it.body else null }
                .map { entity ->
                    val b = entity.bookmark.copy(
                        starCount = starEntries.firstOrNull { s ->
                            s.url.contains("/${entity.bookmark.user}/")
                        }?.allStars
                    )
                    if (!entity.bookmark.same(b)) {
                        updated = true
                    }
                    entity.copy(bookmark = b)
                }
        )

        if (updated) {
            submitList(newStates)
        }
    }

    /** ブクマリストアイテム */
    class ViewHolder(
        private val view: View,
        private val bookmarksAdapter: BookmarksAdapter
    ) : RecyclerView.ViewHolder(view) {
        var bookmark: Entity? = null
            set(value) {
                field = value
                value?.let {
                    init(value)
                }
            }

        private fun init(entity: Entity) {
            val bookmark = entity.bookmark
            val userTags = entity.userTags

            Glide.with(view.context).run {
                clear(view.bookmark_user_icon)
                load(bookmark.userIconUrl)
                    .into(view.bookmark_user_icon)
            }
            view.bookmark_user_name.text = bookmark.user

            view.bookmark_comment.apply {
                text = entity.analyzedComment.comment
                //visibility = (text.isNotEmpty()).toVisibility(View.GONE)

                val linkMovementMethod = object : MutableLinkMovementMethod() {
                    override fun onSinglePressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkClicked(link)
                        }
                        else {
                            entity.analyzedComment.entryIds
                                .firstOrNull { link.contains(it.toString()) }
                                ?.let { eid ->
                                    bookmarksAdapter.onEntryIdClicked(eid)
                                }
                        }
                    }

                    override fun onLongPressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkLongClicked(link)
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

            view.ignored_user_mark.visibility = entity.isIgnored.toVisibility(View.GONE)

            // タグ
            view.bookmark_tags?.also { tagsTextView ->
                if (bookmark.tags.isEmpty()) {
                    tagsTextView.visibility = View.GONE
                }
                else {
                    tagsTextView.visibility = View.VISIBLE
                    val tagsText = bookmark.tags.joinToString(", ")
                    tagsTextView.text = SpannableString("_$tagsText").apply {
                        val resources = tagsTextView.resources
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_tag, null)?.apply {
                            setBounds(0, 0, tagsTextView.lineHeight, tagsTextView.lineHeight)
                            setTint(tagsTextView.currentTextColor)
                        }?.let { icon ->
                            setSpan(ImageSpan(icon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
            }

            // タイムスタンプ & スター
            val builder = SpannableStringBuilder()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            builder.append(bookmark.timestamp.format(formatter))
            builder.append("　")

            if (!bookmark.starCount.isNullOrEmpty()) {
                bookmark.starCount.let { stars ->
                    val yellowStarCount = stars.filter { it.color == StarColor.Yellow }.sumBy { it.count }
                    val redStarCount = stars.filter { it.color == StarColor.Red }.sumBy { it.count }
                    val greenStarCount = stars.filter { it.color == StarColor.Green }.sumBy { it.count }
                    val blueStarCount = stars.filter { it.color == StarColor.Blue }.sumBy { it.count }
                    val purpleStarCount = stars.filter { it.color == StarColor.Purple }.sumBy { it.count }

                    appendStarSpan(builder, purpleStarCount, view.context, R.style.StarSpan_Purple)
                    appendStarSpan(builder, blueStarCount, view.context, R.style.StarSpan_Blue)
                    appendStarSpan(builder, redStarCount, view.context, R.style.StarSpan_Red)
                    appendStarSpan(builder, greenStarCount, view.context, R.style.StarSpan_Green)
                    appendStarSpan(builder, yellowStarCount, view.context, R.style.StarSpan_Yellow)
                }
            }

            // タイムスタンプ部分テキストを設定
            view.bookmark_timestamp.text = builder

            // スターを付けるボタンを設定
            bookmarksAdapter.viewModel.initializeAddStarButton(
                view.context!!,
                bookmarksAdapter.lifecycleOwner,
                view.add_star_button,
                bookmark
            )

            // ユーザータグ
            if (userTags.isNullOrEmpty()) {
                view.user_tags.visibility = View.GONE
            }
            else {
                view.user_tags.let { textView ->
                    val icon = ResourcesCompat.getDrawable(textView.resources, R.drawable.ic_user_tag, null)?.apply {
                        val size = textView.textSize.toInt()
                        setBounds(0, 0, size, size)
                        setTint(textView.currentTextColor)
                    }
                    textView.setCompoundDrawablesRelative(icon, null, null, null)

                    textView.text = userTags.joinToString(", ") { it.name }
                    textView.visibility = View.VISIBLE
                }
            }

            // 言及先リスト
            view.bookmark_mentions.apply {
                val mentions = entity.mentions
                if (mentions.isEmpty()) {
                    visibility = View.GONE
                    adapter = null
                }
                else {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context)
                    adapter = object : MentionsAdapter(mentions) {
                        override fun onItemClicked(item: Bookmark) =
                            bookmarksAdapter.onItemClicked(item)

                        override fun onItemLongClicked(item: Bookmark) =
                            bookmarksAdapter.onItemLongClicked(item)
                    }

                    repeat(itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }

                    setDivider(R.drawable.recycler_view_item_divider)
                }
            }
        }
    }
}
