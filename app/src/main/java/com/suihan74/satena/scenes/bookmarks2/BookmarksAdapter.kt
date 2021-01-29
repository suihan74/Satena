package com.suihan74.satena.scenes.bookmarks2

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewLoadableBinding
import com.suihan74.satena.databinding.ListviewItemBookmarksBinding
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.appendStarSpan
import com.suihan74.utilities.extensions.toSystemZonedDateTime
import com.suihan74.utilities.extensions.toVisibility
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.threeten.bp.format.DateTimeFormatter

fun <T> List<T>?.contentsEquals(other: List<T>?, comparator: ((T, T)->Boolean) = { a, b -> a == b }) =
    if (this == null && other == null) true
    else if (this == null && other != null) false
    else if (this != null && other == null) false
    else this!!.size == other!!.size && this.mapIndexed { index, _ -> comparator(this[index], other[index]) }.all { it }


class BookmarksAdapter(
    cache: List<RecyclerState<Entity>>? = null
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
                    mentions.contentsEquals(other.mentions) { a, b -> a.same(b) } &&
                    userTags.contentsEquals(other.userTags) { a, b -> a.name == b.name }
        }

        override fun hashCode() = super.hashCode()
    }

    /** 表示項目リスト */
    private var loadableFooter: LoadableFooterViewHolder? = null

    private var onSubmitted: Listener<List<RecyclerState<Entity>>>? = null

    /** ブクマ項目クリック時処理 */
    private var onItemClicked: Listener<Bookmark>? = null

    /** ブクマ項目ロングクリック時処理 */
    private var onItemLongClicked: Listener<Bookmark>? = null

    /** コメント中のリンクをタップしたときの処理 */
    private var onLinkClicked: Listener<String>? = null

    /** コメント中のリンクをロングタップしたときの処理 */
    private var onLinkLongClicked: Listener<String>? = null

    /** コメント中のEntryIdをタップしたときの処理 */
    private var onEntryIdClicked: Listener<Long>? = null

    /** コメント中のEntryIdをロングタップしたときの処理 */
    private var onEntryIdLongClicked: Listener<Long>? = null

    /** フッターの追加ロードをタップしたときの処理 */
    private var onAdditionalLoading: Listener<Unit>? = null

    /** スターをつける処理をボタンに設定する */
    private var addStarButtonBinder : ((button: ImageButton, bookmark: Bookmark)->Unit)? = null

    fun setOnSubmitListener(listener: Listener<List<RecyclerState<Entity>>>?) {
        onSubmitted = listener
    }

    fun setOnItemClickedListener(listener: Listener<Bookmark>?) {
        onItemClicked = listener
    }

    fun setOnItemLongClickedListener(listener: Listener<Bookmark>?) {
        onItemLongClicked = listener
    }

    /** コメント中のリンクをタップしたときの処理 */
    fun setOnLinkClickedListener(listener: Listener<String>?) {
        onLinkClicked = listener
    }

    /** コメント中のリンクをロングタップしたときの処理 */
    fun setOnLinkLongClickedListener(listener: Listener<String>?) {
        onLinkLongClicked = listener
    }

    /** コメント中のEntryIdをタップしたときの処理 */
    fun setOnEntryIdClickedListener(listener: Listener<Long>?) {
        onEntryIdClicked = listener
    }
    /** コメント中のEntryIdをタップしたときの処理 */
    fun setOnEntryIdLongClickedListener(listener: Listener<Long>?) {
        onEntryIdLongClicked = listener
    }


    /** フッターの追加ロードをタップしたときの処理 */
    fun setOnAdditionalLoadingListener(listener: Listener<Unit>?) {
        onAdditionalLoading = listener
    }

    /** スターをつける処理をボタンに設定する */
    fun setAddStarButtonBinder(binder: ((button: ImageButton, bookmark: Bookmark)->Unit)?) {
        addStarButtonBinder = binder
    }

    /** 追加ロードボタンを表示するか */
    var additionalLoadable: Boolean = false
        set (value) {
            field = value
            loadableFooter?.additionalLoadingTextView?.setVisibility(value)
        }

    override fun getItemCount() = currentList.size

    override fun getItemViewType(position: Int) =
        currentList[position].type.id

    /**
     * 指定ブクマの位置を取得する
     * @return 対象が存在するとき ---> そのインデックス
     *
     * 対象が存在しないとき ---> -1
     */
    fun getPosition(bookmark: Bookmark) =
        currentList.indexOfFirst { it.body?.bookmark?.user == bookmark.user }

    init {
        // キャッシュが存在する場合、その内容を引き継ぐ
        // 画面回転のたびにリストが再生成されるのを防ぐために行っている
        if (!cache.isNullOrEmpty()) {
            submitList(cache)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromId(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(
                        ListviewItemBookmarksBinding.inflate(
                            inflater,
                            parent,
                            false
                        ),
                        this
                    )

                RecyclerType.FOOTER -> loadableFooter ?:
                    LoadableFooterViewHolder(
                        FooterRecyclerViewLoadableBinding.inflate(
                            inflater,
                            parent,
                            false
                        )
                    ).also {
                        loadableFooter = it.also { footer ->
                            footer.additionalLoadingTextView.let { textView ->
                                textView.setOnClickListener {
                                    onAdditionalLoading?.invoke(Unit)
                                }
                                textView.setVisibility(additionalLoadable)
                            }
                        }
                    }

                else -> throw RuntimeException("an invalid list item")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).run {
                    val entity = currentList[position].body!!
                    bookmark = entity

                    itemView.setOnClickListener {
                        val bookmark = entity.bookmark
                        onItemClicked?.invoke(bookmark)
                    }
                    itemView.setOnLongClickListener {
                        val bookmark = entity.bookmark
                        onItemLongClicked?.invoke(bookmark)
                        onItemLongClicked != null
                    }
                }
            }

            else -> Unit
        }

    /**
     * フッタのローディングアニメを表示する
     *
     * @return ロード処理中ではない場合true
     */
    fun startLoading() : Boolean {
        val result = loadableFooter?.progressBar?.isVisible != true
        additionalLoadable = false
        loadableFooter?.showProgressBar()
        return result
    }

    /** フッタのローディングアニメを隠す */
    fun stopLoading(additionalLoadable: Boolean = this.additionalLoadable) {
        this.additionalLoadable = additionalLoadable
        loadableFooter?.hideProgressBar(additionalLoadable)
    }

    private val setBookmarksLock = Mutex()

    fun setBookmarks(entities: List<RecyclerState<Entity>>) {
        submitList(entities) {
            onSubmitted?.invoke(entities)
        }
    }

    suspend fun setBookmarks(
        bookmarks: List<Bookmark>,
        bookmarksEntry: BookmarksEntry?,
        taggedUsers: List<UserAndTags>,
        ignoredUsers: List<String>,
        displayMutedMention: Boolean,
        onSubmitted: Listener<List<RecyclerState<Entity>>>? = null
    ) = withContext(Dispatchers.Main) {
        if (!setBookmarksLock.tryLock()) return@withContext

        try {
            val newStates = withContext(Dispatchers.Default) {
                RecyclerState.makeStatesWithFooter(bookmarks.map { bookmark ->
                    val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
                    Entity(
                        bookmark = bookmark,
                        analyzedComment = analyzedComment,
                        isIgnored = ignoredUsers.contains(bookmark.user),
                        mentions = analyzedComment.ids.mapNotNull { called ->
                            bookmarksEntry?.bookmarks?.firstOrNull { b -> b.user == called }
                                ?.let { mentioned ->
                                    if (!displayMutedMention && ignoredUsers.contains(mentioned.user)) null
                                    else mentioned
                                }
                        },
                        userTags = taggedUsers.firstOrNull { t -> t.user.name == bookmark.user }?.tags
                            ?: emptyList()
                    )
                })
            }

            submitList(newStates) {
                onSubmitted?.invoke(newStates)
                setBookmarksLock.unlock()
            }
        }
        catch (e: CancellationException) {
            Log.i("BookmarksAdapter", "#setBookmarks() is canceled")
            setBookmarksLock.unlock()
        }
        catch (e: Throwable) {
            Log.e("BookmarksAdapter", Log.getStackTraceString(e))
            setBookmarksLock.unlock()
        }
    }

    /** スター情報を更新 */
    suspend fun updateStars(entry: Entry, stars: List<StarsEntry>) {
        val entities = currentList.filter { it.type == RecyclerType.BODY }.mapNotNull { it.body }
        val newStates = RecyclerState.makeStatesWithFooter(
            entities.map { entity ->
                val bookmark = entity.bookmark
                val bookmarkUrl = bookmark.getBookmarkUrl(entry)
                val star = stars.firstOrNull { it.url == bookmarkUrl }

                if (star == null) entity
                else entity.copy(bookmark = bookmark.copy(starCount = star.allStars))
            }
        )

        withContext(Dispatchers.Main) {
            submitList(newStates)
        }
    }

    /** ブクマリストアイテム */
    class ViewHolder(
        private val binding: ListviewItemBookmarksBinding,
        private val bookmarksAdapter: BookmarksAdapter
    ) : RecyclerView.ViewHolder(binding.root) {
        var bookmark: Entity? = null
            set(value) {
                field = value
                value?.let {
                    init(value)
                }
            }

        @SuppressLint("ClickableViewAccessibility")
        private fun init(entity: Entity) {
            val bookmark = entity.bookmark
            val userTags = entity.userTags

            val context = binding.root.context

            Glide.with(context).run {
                clear(binding.bookmarkUserIcon)
                load(bookmark.userIconUrl)
                    .into(binding.bookmarkUserIcon)
            }
            binding.bookmarkUserName.text = bookmark.user

            binding.bookmarkComment.apply {
                text = entity.analyzedComment.comment
                //visibility = (text.isNotEmpty()).toVisibility(View.GONE)

                val linkMovementMethod = object : MutableLinkMovementMethod() {
                    override fun onSinglePressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkClicked?.invoke(link)
                        }
                        else {
                            entity.analyzedComment.entryIds
                                .firstOrNull { link.contains(it.toString()) }
                                ?.let { eid ->
                                    bookmarksAdapter.onEntryIdClicked?.invoke(eid)
                                }
                        }
                    }

                    override fun onLongPressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkLongClicked?.invoke(link)
                        }
                        else {
                            entity.analyzedComment.entryIds
                                .firstOrNull { link.contains(it.toString()) }
                                ?.let { eid ->
                                    bookmarksAdapter.onEntryIdLongClicked?.invoke(eid)
                                }
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

            binding.ignoredUserMark.visibility = entity.isIgnored.toVisibility(View.GONE)

            // タグ
            binding.bookmarkTags.also { tagsTextView ->
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
            builder.append(
                bookmark.timestamp
                    .toSystemZonedDateTime("Asia/Tokyo")
                    .format(DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm"))
            )
            builder.append("　")

            if (!bookmark.starCount.isNullOrEmpty()) {
                bookmark.starCount.let { stars ->
                    val yellowStarCount = stars.filter { it.color == StarColor.Yellow }.sumBy { it.count }
                    val redStarCount = stars.filter { it.color == StarColor.Red }.sumBy { it.count }
                    val greenStarCount = stars.filter { it.color == StarColor.Green }.sumBy { it.count }
                    val blueStarCount = stars.filter { it.color == StarColor.Blue }.sumBy { it.count }
                    val purpleStarCount = stars.filter { it.color == StarColor.Purple }.sumBy { it.count }

                    builder.appendStarSpan(purpleStarCount, context, R.style.StarSpan_Purple)
                    builder.appendStarSpan(blueStarCount, context, R.style.StarSpan_Blue)
                    builder.appendStarSpan(redStarCount, context, R.style.StarSpan_Red)
                    builder.appendStarSpan(greenStarCount, context, R.style.StarSpan_Green)
                    builder.appendStarSpan(yellowStarCount, context, R.style.StarSpan_Yellow)
                }
            }

            // タイムスタンプ部分テキストを設定
            binding.bookmarkTimestamp.text = builder

            // スターを付けるボタンを設定
            bookmarksAdapter.addStarButtonBinder?.invoke(binding.addStarButton, bookmark)

            // ユーザータグ
            if (userTags.isNullOrEmpty()) {
                binding.userTags.visibility = View.GONE
            }
            else {
                binding.userTags.let { textView ->
                    val icon = ResourcesCompat.getDrawable(
                        textView.resources,
                        R.drawable.ic_user_tag,
                        null
                    )?.apply {
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
            binding.bookmarkMentions.apply {
                val mentions = entity.mentions
                if (mentions.isEmpty()) {
                    visibility = View.GONE
                    adapter = null
                }
                else {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context)
                    adapter = object : MentionsAdapter(mentions) {
                        override fun onItemClicked(item: Bookmark) {
                            bookmarksAdapter.onItemClicked?.invoke(item)
                        }

                        override fun onItemLongClicked(item: Bookmark) : Boolean {
                            bookmarksAdapter.onItemLongClicked?.invoke(item)
                            return bookmarksAdapter.onItemLongClicked != null
                        }
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
