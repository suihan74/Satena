package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.text.method.LinkMovementMethod
import android.transition.*
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.*
import com.suihan74.satena.R
import com.suihan74.satena.adapters.tabs.StarsTabAdapter
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.TextFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkDetailFragment : CoroutineScopeFragment(), BackPressable {
    private lateinit var mView : View
    private lateinit var mBookmark : Bookmark
    private lateinit var mStarsMap: Map<String, StarsEntry>
    private lateinit var mBookmarksEntry : BookmarksEntry

    private lateinit var mColorStars : UserColorStarsCount

    private var mIsStarMenuOpened : Boolean = false

    companion object {
        fun createInstance(b: Bookmark, s: Map<String, StarsEntry>, bEntry: BookmarksEntry) = BookmarkDetailFragment().apply {
            mBookmark = b
            mStarsMap = s
            mBookmarksEntry = bEntry

            enterTransition = TransitionSet()
                .addTransition(Fade())
                .addTransition(Slide(Gravity.END))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark_detail, container, false)
        mView = view

        val user = view.findViewById<TextView>(R.id.user_name)
        val icon = view.findViewById<ImageView>(R.id.user_icon)
        val comment = view.findViewById<TextView>(R.id.comment)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val tabPager = view.findViewById<ViewPager>(R.id.tab_pager)

        if (mBookmark.tags.isNullOrEmpty()) {
            view.findViewById<View>(R.id.tags_layout).visibility = View.GONE
        }
        else {
            view.findViewById<View>(R.id.tags_layout).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tags).apply {
                text = BookmarkCommentDecorator.makeClickableTagsText(mBookmark.tags) { tag ->
                    val fragment = SearchEntriesFragment.createInstance(tag, SearchType.Tag)
                    (activity as FragmentContainerActivity).showFragment(fragment, null)
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

            val showStarsButton = mView.findViewById<TextFloatingActionButton>(R.id.show_stars_button).apply {
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
                        activity?.showToast("所持スター数の取得に失敗しました")
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

        tabPager.adapter = StarsTabAdapter(
            childFragmentManager,
            mBookmark,
            mStarsMap,
            mBookmarksEntry
        )
        tabLayout.setupWithViewPager(tabPager)

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
                    .setMessage("${color.name.toUpperCase()}スターをつけます。よろしいですか？")
                    .setPositiveButton("OK") { _, _ -> postStarImpl(color, count) }
                    .setNegativeButton("CANCEL", null)
                    .show()
            }
            else {
                postStarImpl(color, count)
            }
        }
        else {
            activity?.showToast("${color.name.toUpperCase()}スターを所持していません")
        }
    }

    private fun postStarImpl(color: StarColor, count: Int) {
        if (color == StarColor.Yellow || count > 0) {
            launch(Dispatchers.Main) {
                try {
                    val quote = mView.findViewById<TextView>(R.id.quote_text_view).text.toString()

                    HatenaClient.postStarAsync(
                        url = mBookmark.getBookmarkUrl(mBookmarksEntry),
                        color = color,
                        quote = quote
                    ).await()

                    activity?.showToast("${mBookmark.user}のブコメに★をつけました")
                }
                catch (e: Exception) {
                    activity?.showToast("${mBookmark.user}のブコメに★をつけるのに失敗しました")
                    Log.d("failedToPostStar", Log.getStackTraceString(e))
                }
            }
        }
        else {
            activity?.showToast("${color.name.toUpperCase()}スターを所持していません")
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
