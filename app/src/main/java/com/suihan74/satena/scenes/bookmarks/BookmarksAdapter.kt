package com.suihan74.satena.scenes.bookmarks

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewLoadableBinding
import com.suihan74.satena.databinding.ListviewItemBookmarksBinding
import com.suihan74.satena.databinding.ListviewSectionBookmarkBinding
import com.suihan74.satena.scenes.bookmarks.viewModel.Entity
import com.suihan74.utilities.*
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.appendStarSpan
import com.suihan74.utilities.extensions.toSystemZonedDateTime
import com.suihan74.utilities.extensions.toVisibility
import kotlinx.coroutines.*
import org.threeten.bp.format.DateTimeFormatter

class BookmarksAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<RecyclerState<Entity>, RecyclerView.ViewHolder>(Entity.DiffCallback()) {

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
    private var addStarButtonBinder : ((button: ImageButton, edge: View, bookmark: Bookmark)->Unit)? = null

    // ------ //

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
    fun setAddStarButtonBinder(binder: ((button: ImageButton, edge: View, bookmark: Bookmark)->Unit)?) {
        addStarButtonBinder = binder
    }

    /** Footer: ロード中の表示用 */
    val loading = MutableLiveData<Boolean>()

    /** Footer: 追加ロードボタンを表示するか */
    val loadable = MutableLiveData<Boolean>()

    // ------ //

    override fun getItemCount() = currentList.size

    override fun getItemViewType(position: Int) =
        currentList[position].type.id

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
                        this,
                        lifecycleOwner
                    )

                RecyclerType.SECTION ->
                    SectionViewHolder(
                        ListviewSectionBookmarkBinding.inflate(
                            inflater,
                            parent,
                            false
                        )
                    )

                RecyclerType.FOOTER ->
                    LoadableFooterViewHolder(
                        FooterRecyclerViewLoadableBinding.inflate(
                            inflater,
                            parent,
                            false
                        ).also {
                            it.loading = loading
                            it.loadable = loadable
                            it.lifecycleOwner = lifecycleOwner
                            it.loadTextView.setOnClickListener {
                                loading.value = true
                                onAdditionalLoading?.invoke(Unit)
                            }
                        }
                    )

                else -> throw RuntimeException("an invalid list item")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = currentList[position]
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).run {
                    val entity = item.body!!
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

            RecyclerType.SECTION -> holder.alsoAs<SectionViewHolder> { vh ->
                vh.binding.alsoAs<ListviewSectionBookmarkBinding> { binding ->
                    item.extra.alsoAs<Int> {
                        binding.textView.text = binding.root.context.getString(it)
                    }
                }
            }

            else -> Unit
        }
    }

    /**
     * フッタのローディングアニメを表示する
     *
     * @return ロード処理中ではない場合true
     */
    fun startLoading() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            loading.value = true
        }
    }

    /** フッタのローディングアニメを隠す */
    fun stopLoading() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            loading.value = false
        }
    }

    fun setBookmarks(entities: List<RecyclerState<Entity>>?) {
        submitList(entities) {
            lifecycleOwner.lifecycleScope.launchWhenResumed {
                onSubmitted?.invoke(entities ?: emptyList())
            }
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
        private val bookmarksAdapter: BookmarksAdapter,
        private val lifecycleOwner: LifecycleOwner
    ) : RecyclerView.ViewHolder(binding.root) {
        private var bookmarkCountObserver: Observer<Int>? = null

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

            binding.privateMark.visibility = entity.bookmark.private.toVisibility()
            binding.ignoredUserMark.visibility = entity.isIgnored.toVisibility()

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
                    val yellowStarCount = stars.filter { it.color == StarColor.Yellow }.sumOf { it.count }
                    val redStarCount = stars.filter { it.color == StarColor.Red }.sumOf { it.count }
                    val greenStarCount = stars.filter { it.color == StarColor.Green }.sumOf { it.count }
                    val blueStarCount = stars.filter { it.color == StarColor.Blue }.sumOf { it.count }
                    val purpleStarCount = stars.filter { it.color == StarColor.Purple }.sumOf { it.count }

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
            if (bookmark.commentRaw.isNotBlank()) {
                binding.addStarButton.visibility = View.VISIBLE
                binding.addStarEdge.visibility = View.VISIBLE
                bookmarksAdapter.addStarButtonBinder?.invoke(binding.addStarButton, binding.addStarEdge, bookmark)
            }
            else {
                binding.addStarButton.visibility = View.GONE
                binding.addStarEdge.visibility = View.GONE
            }

            // ブクマへのブクマ数の表示
            bookmarkCountObserver?.let {
                entity.bookmarkCount.removeObserver(it)
            }
            bookmarkCountObserver = Observer { count ->
                if (count != null && count > 0) {
                    binding.bookmarksCount.text = "%d user%s".format(
                        count,
                        if (count == 1) "" else "s"
                    )
                    binding.bookmarksCount.visibility = View.VISIBLE
                }
                else {
                    binding.bookmarksCount.text = ""
                    binding.bookmarksCount.visibility = View.GONE
                }
            }
            entity.bookmarkCount.observe(lifecycleOwner, bookmarkCountObserver!!)

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
