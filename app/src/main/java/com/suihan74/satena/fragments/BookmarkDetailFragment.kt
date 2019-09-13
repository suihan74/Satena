package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.text.method.LinkMovementMethod
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.adapters.tabs.StarsTabAdapter
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class BookmarkDetailFragment : CoroutineScopeFragment(), BackPressable {
    private lateinit var mView : View
    private lateinit var mBookmark : Bookmark

    private lateinit var mColorStars : UserColorStarsCount

    private var mIsStarMenuOpened : Boolean = false

    private var mBookmarksFragment: BookmarksFragment? = null

    val bookmark
        get() = mBookmark

    companion object {
        fun createInstance(b: Bookmark) = BookmarkDetailFragment().apply {
            mBookmark = b

            enterTransition = TransitionSet()
                .addTransition(Fade())
                .addTransition(Slide(Gravity.END))
        }

        private const val BUNDLE_BOOKMARK = "mBookmark"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putSerializable(BUNDLE_BOOKMARK, mBookmark)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            mBookmark = it.getSerializable(BUNDLE_BOOKMARK) as Bookmark
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark_detail, container, false)
        mView = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksDetailFragment has created from an invalid activity")

        val user = view.findViewById<TextView>(R.id.user_name)
        val icon = view.findViewById<ImageView>(R.id.user_icon)
        val comment = view.findViewById<TextView>(R.id.comment)

        if (mBookmark.tags.isNullOrEmpty()) {
            view.findViewById<View>(R.id.tags_layout).visibility = View.GONE
        }
        else {
            view.findViewById<View>(R.id.tags_layout).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tags).apply {
                text = BookmarkCommentDecorator.makeClickableTagsText(mBookmark.tags) { tag ->
                    val fragment = SearchEntriesFragment.createInstance(tag, SearchType.Tag)
                    activity.showFragment(fragment, null)
                }
                movementMethod = LinkMovementMethod.getInstance()
            }
        }

        val starMenuButton = view.findViewById<TextFloatingActionButton>(R.id.show_stars_button)
        if (HatenaClient.signedIn()) {
            starMenuButton.setOnClickListener {
                starMenuButton.show()
                if (mIsStarMenuOpened) {
                    closeStarMenu()
                } else {
                    openStarMenu()
                }
            }

            view.findViewById<TextFloatingActionButton>(R.id.yellow_star_button).setOnClickListener {
                postStar(StarColor.Yellow, Int.MAX_VALUE)
            }
            view.findViewById<TextFloatingActionButton>(R.id.red_star_button).setOnClickListener {
                postStar(StarColor.Red, mColorStars.red)
            }
            view.findViewById<TextFloatingActionButton>(R.id.green_star_button).setOnClickListener {
                postStar(StarColor.Green, mColorStars.green)
            }
            view.findViewById<TextFloatingActionButton>(R.id.blue_star_button).setOnClickListener {
                postStar(StarColor.Blue, mColorStars.blue)
            }
            view.findViewById<TextFloatingActionButton>(R.id.purple_star_button).setOnClickListener {
                postStar(StarColor.Purple, mColorStars.purple)
            }

            val showStarsButton = mView.findViewById<View>(R.id.show_stars_button).apply {
                visibility = View.GONE
            }

            if (HatenaClient.signedIn()) {
                launch(Dispatchers.Main) {
                    try {
                        mColorStars = HatenaClient.getMyColorStarsAsync().await()
                    }
                    catch (e: Exception) {
                        mColorStars = UserColorStarsCount(red = 0, green = 0, blue = 0, purple = 0)
                        Log.d("failedToFetchStars", Log.getStackTraceString(e))
                        activity.showToast("所持スター数の取得に失敗しました")
                    }
                    finally {
                        mView.apply {
                            findViewById<TextView>(R.id.red_stars_count).text = mColorStars.red.toString()
                            findViewById<TextView>(R.id.green_stars_count).text = mColorStars.green.toString()
                            findViewById<TextView>(R.id.blue_stars_count).text = mColorStars.blue.toString()
                            findViewById<TextView>(R.id.purple_stars_count).text = mColorStars.purple.toString()
                        }
                        showStarsButton.visibility = View.VISIBLE
                    }
                }
            }
        }
        else {
            starMenuButton.hide()
        }

        val analyzed = BookmarkCommentDecorator.convert(mBookmark.comment)

        user.text = mBookmark.user
        comment.apply {
            text = analyzed.comment
            movementMethod = LinkMovementMethod.getInstance()
            customSelectionActionModeCallback = object : ActionMode.Callback {
                private val quote = view.findViewById<TextView>(R.id.quote_text_view)

                override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) : Boolean {
                    val selectedText = comment.text.substring(comment.selectionStart, comment.selectionEnd)
                    quote.apply {
                        text = String.format("\"%s\"", selectedText)
                        visibility = View.VISIBLE
                    }
                    return false
                }
                override fun onDestroyActionMode(p0: ActionMode?) {
                    quote.apply {
                        text = ""
                        visibility = View.INVISIBLE
                    }
                }

                override fun onCreateActionMode(p0: ActionMode?, p1: Menu?) = true
                override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?) = false
            }
        }

        Glide.with(view)
            .load(mBookmark.userIconUrl)
            .into(icon)

        retainInstance = true
        return view
    }

    override fun onResume() {
        super.onResume()

        val tabLayout = mView.findViewById<TabLayout>(R.id.tab_layout)
        val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)

        launch(Dispatchers.Main) {
            val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksDetailFragment has created from an invalid activity")
            mBookmarksFragment = activity.bookmarksFragment

            val task = mBookmarksFragment!!.getFetchStarsTask(mBookmark.user)
            if (task == null || task.isCompleted) {
                val adapter = StarsTabAdapter(
                    tabPager,
                    mBookmarksFragment!!,
                    this@BookmarkDetailFragment,
                    mBookmark
                )
                tabPager.adapter = adapter

                tabLayout.run {
                    setupWithViewPager(tabPager)
                    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(p0: TabLayout.Tab?) {}
                        override fun onTabUnselected(p0: TabLayout.Tab?) {}
                        override fun onTabReselected(tab: TabLayout.Tab?) {
                            val fragment = adapter.findFragment(tab!!.position)
                            fragment.scrollToTop()
                        }
                    })
                }
            } else {
                val progressBar = mView.findViewById<ProgressBar>(R.id.detail_progress_bar).apply {
                    visibility = View.VISIBLE
                }

                try {
                    task.await()
                    tabPager.adapter = StarsTabAdapter(
                        tabPager,
                        mBookmarksFragment!!,
                        this@BookmarkDetailFragment,
                        mBookmark
                    )
                    tabLayout.setupWithViewPager(tabPager)
                } catch (e: Exception) {
                    Log.d("FailedToFetchStars", e.message)
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showStarButton(layoutId: Int, counterId: Int, dimenId: Int) {
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)
        val pos = context!!.resources.getDimension(dimenId)

        layout.animate()
            .withEndAction {
                counter.animate()
                    .translationXBy(100f)
                    .translationX(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationYBy(0f)
            .translationY(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    private fun hideStarButton(layoutId: Int, counterId: Int) {
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)

        counter.animate()
            .withEndAction {
                layout.animate()
                    .translationY(0f)
                    .alpha(0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }

    private fun openStarMenu() {
        mIsStarMenuOpened = true
        showStarButton(
            R.id.purple_star_layout,
            R.id.purple_stars_count,
            R.dimen.purple_star_position
        )
        showStarButton(
            R.id.blue_star_layout,
            R.id.blue_stars_count,
            R.dimen.blue_star_position
        )
        showStarButton(
            R.id.red_star_layout,
            R.id.red_stars_count,
            R.dimen.red_star_position
        )
        showStarButton(
            R.id.green_star_layout,
            R.id.green_stars_count,
            R.dimen.green_star_position
        )
        showStarButton(
            R.id.yellow_star_layout,
            R.id.yellow_stars_count,
            R.dimen.yellow_star_position
        )

        mView.findViewById<TextFloatingActionButton>(R.id.show_stars_button).text = "×"
    }

    private fun closeStarMenu() {
        mIsStarMenuOpened = false
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        mView.findViewById<TextFloatingActionButton>(R.id.show_stars_button).text = "★"
    }

    private fun postStar(color: StarColor, count: Int) {
        if (color == StarColor.Yellow || count > 0) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
            val usingDialog = prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
            if (usingDialog) {
                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle("確認")
                    .setMessage("${color.name.toUpperCase(Locale.ROOT)}スターをつけます。よろしいですか？")
                    .setPositiveButton("OK") { _, _ -> postStarImpl(color, count) }
                    .setNegativeButton("CANCEL", null)
                    .show()
            }
            else {
                postStarImpl(color, count)
            }
        }
        else {
            activity?.showToast("${color.name.toUpperCase(Locale.ROOT)}スターを所持していません")
        }
    }

    private fun postStarImpl(color: StarColor, count: Int) {
        if (color == StarColor.Yellow || count > 0) {
            launch(Dispatchers.Main) {
                try {
                    val quote = mView.findViewById<TextView>(R.id.quote_text_view).text.toString()

                    HatenaClient.postStarAsync(
                        url = mBookmark.getBookmarkUrl(mBookmarksFragment!!.bookmarksEntry!!),
                        color = color,
                        quote = quote
                    ).await()

                    mBookmarksFragment!!.updateStar(mBookmark)

                    val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)
                    tabPager.adapter = StarsTabAdapter(
                        tabPager,
                        mBookmarksFragment!!,
                        this@BookmarkDetailFragment,
                        mBookmark
                    )

                    activity?.showToast("${mBookmark.user}のブコメに★をつけました")
                }
                catch (e: Exception) {
                    activity?.showToast("${mBookmark.user}のブコメに★をつけるのに失敗しました")
                    Log.d("failedToPostStar", Log.getStackTraceString(e))
                }
            }
        }
        else {
            activity?.showToast("${color.name.toUpperCase(Locale.ROOT)}スターを所持していません")
        }
    }

    fun updateStars() = launch {
        mBookmarksFragment!!.updateStar(mBookmark)

        withContext(Dispatchers.Main) {
            val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)
            tabPager.adapter = StarsTabAdapter(
                tabPager,
                mBookmarksFragment!!,
                this@BookmarkDetailFragment,
                mBookmark
            )
        }
    }

    override fun onBackPressed(): Boolean {
        if (mIsStarMenuOpened) {
            closeStarMenu()
            return true
        }
        return false
    }
}
