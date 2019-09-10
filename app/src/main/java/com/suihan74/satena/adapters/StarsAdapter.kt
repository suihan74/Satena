package com.suihan74.satena.adapters

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.models.IgnoreTarget
import com.suihan74.satena.models.IgnoredEntriesKey
import com.suihan74.satena.models.IgnoredEntry
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*

open class StarsAdapter(
    private val context : Context,
    private val bookmark : Bookmark,
    private var starsMap : Map<String, StarsEntry>,
    private val bookmarks : List<Bookmark>,
    private val mode : StarsTabMode
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class StarsTabMode {
        TO_USER,
        FROM_USER
    }

    private val muteWords : List<String>

    private var stars : List<Star> = createStarsListToUser()
    private val staredBookmarks : List<Bookmark> = createBookmarksListStaredFromUser()

    private var statesModeTo = RecyclerState.makeStatesWithFooter(stars)
    private val statesModeFrom = RecyclerState.makeStatesWithFooter(staredBookmarks)


    init {
        val pref = SafeSharedPreferences.create<IgnoredEntriesKey>(context)
        pref.get<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES).let {
            muteWords = it
                .filter { e -> e.target contains IgnoreTarget.BOOKMARK }
                .map { e -> e.query }
        }
    }


    private fun createStarsListToUser() : List<Star> {
        if (mode != StarsTabMode.TO_USER) return emptyList()

        val starsEntry = starsMap[bookmark.user] ?: StarsEntry("", emptyList(), null)
        val list = starsEntry.allStars

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val showingIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)

        return if (showingIgnoredUsers) {
            list
        }
        else {
            list.filter { !HatenaClient.ignoredUsers.contains(it.user) }
        }
    }

    private fun createBookmarksListStaredFromUser() : List<Bookmark> {
        if (mode != StarsTabMode.FROM_USER) return emptyList()

        val list = starsMap.values
            .filter { entry -> entry.stars.firstOrNull { star -> star.user == bookmark.user } != null }
            .mapNotNull { entry -> bookmarks.firstOrNull { bookmark -> entry.url.contains("/${bookmark.user}/") }}

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val showingIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)

        return if (showingIgnoredUsers) {
            list
        }
        else {
            list.filter { !HatenaClient.ignoredUsers.contains(it.user) }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_stars, parent, false)
                val holder = ViewHolder(inflate, muteWords)

                holder.itemView.setOnClickListener {
                    onItemClicked(holder.user!!, holder.star)
                }

                holder.itemView.setOnLongClickListener {
                    onItemLongClicked(holder.user!!, holder.star)
                }

                return holder
            }

            RecyclerType.FOOTER -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                return FooterViewHolder(inflate)
            }

            else -> throw RuntimeException("invalid RecyclerType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                holder as ViewHolder
                when (mode) {
                    StarsTabMode.TO_USER -> {
                        val star = statesModeTo[position].body!!
                        val b = bookmarks.find { it.user == star.user }
                        holder.setStar(star, b)
                    }

                    StarsTabMode.FROM_USER -> {
                        val b = statesModeFrom[position].body!!
                        val starsEntry = starsMap[b.user]
                        holder.setBookmark(b, bookmark.user, starsEntry!!)
                    }
                }
            }

            else -> {}
        }
    }

    fun removeItem(user: String) {
        when(mode) {
            StarsTabMode.TO_USER -> {
                val position = statesModeTo.indexOfFirst { it.type == RecyclerType.BODY && it.body!!.user == user }
                statesModeTo.removeAt(position)
                notifyItemRemoved(position)
            }
            StarsTabMode.FROM_USER -> {
                val position = statesModeFrom.indexOfFirst { it.type == RecyclerType.BODY && it.body!!.user == user }
                statesModeFrom.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun getItemCount() = when (mode) {
        StarsTabMode.TO_USER -> statesModeTo.size
        StarsTabMode.FROM_USER -> statesModeFrom.size
    }

    override fun getItemViewType(position: Int): Int = when (mode) {
        StarsTabMode.TO_USER -> statesModeTo[position].type.int
        StarsTabMode.FROM_USER -> statesModeFrom[position].type.int
    }

    open fun onItemClicked(user: String, star: Star?) {}
    open fun onItemLongClicked(user: String, star: Star?) : Boolean = true


    class ViewHolder(private val view : View, private val muteWords: List<String>) : RecyclerView.ViewHolder(view) {
        private val userName    = view.findViewById<TextView>(R.id.star_user_name)!!
        private val userIcon    = view.findViewById<ImageView>(R.id.star_user_icon)!!
        private val comment     = view.findViewById<TextView>(R.id.star_comment)!!
        private val starsCount  = view.findViewById<TextView>(R.id.star_stars_count)!!

        var user : String? = null
            private set

        var star : Star? = null
            private set

        fun setStar(star: Star, bookmark: Bookmark?) {
            this.star = star
            user = star.user

            userName.text = user

            val colorId = when (star.color) {
                StarColor.Yellow -> R.color.starYellow
                StarColor.Red -> R.color.starRed
                StarColor.Green -> R.color.starGreen
                StarColor.Blue -> R.color.starBlue
                StarColor.Purple -> R.color.starPurple
            }
            val starColor = ContextCompat.getColor(view.context, colorId)
            val starText = if (star.count > 9) "★${star.count}" else {
                buildString {
                    for (i in 1..star.count) append("★")
                }
            }

            starsCount.setHtml("<font color=\"$starColor\">$starText</font>")

            val bookmarkComment = BookmarkCommentDecorator.convert(bookmark?.comment ?: "").comment
            val isMuted = muteWords.any { word -> bookmarkComment.contains(word) }
            comment.text = if (isMuted) view.context.getString(R.string.muted_comment_description) else bookmarkComment

            Glide.with(view)
                .load(star.userIconUrl)
                .into(userIcon)
        }

        fun setBookmark(bookmark: Bookmark, targetUser: String, starsEntry: StarsEntry) {
            star = starsEntry.stars.firstOrNull { user == targetUser }
            user = bookmark.user

            userName.text = user
            starsCount.text = String.format("★%d", starsEntry.totalStarsCount)

            val bookmarkComment = BookmarkCommentDecorator.convert(bookmark.comment).comment
            val isMuted = muteWords.any { word -> bookmarkComment.contains(word) }
            comment.text = if (isMuted) view.context.getString(R.string.muted_comment_description) else bookmarkComment

            Glide.with(view)
                .load(bookmark.userIconUrl)
                .into(userIcon)
        }
    }
}
